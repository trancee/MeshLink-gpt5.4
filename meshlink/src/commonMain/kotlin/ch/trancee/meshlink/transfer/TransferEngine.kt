package ch.trancee.meshlink.transfer

import ch.trancee.meshlink.api.DiagnosticCode
import ch.trancee.meshlink.api.DiagnosticPayload
import ch.trancee.meshlink.api.DiagnosticSink
import ch.trancee.meshlink.api.NoOpDiagnosticSink
import ch.trancee.meshlink.api.PeerIdHex

/** Coordinates outbound transfer sessions and inbound chunk reassembly. */
public class TransferEngine(
  public val config: TransferConfig,
  private val chunkSizePolicy: ChunkSizePolicy,
  private val scheduler: TransferScheduler = TransferScheduler(),
  private val diagnosticSink: DiagnosticSink = NoOpDiagnosticSink,
) {
  private val sessionsByTransferId: MutableMap<String, ManagedTransferSession> = linkedMapOf()
  private val inboundChunksByTransferId: MutableMap<String, MutableMap<Int, ByteArray>> =
    mutableMapOf()
  private val inboundExpectedChunkCounts: MutableMap<String, Int> = mutableMapOf()

  /** Starts a new outbound transfer and queues it with the scheduler. */
  public fun startTransfer(
    transferId: String,
    recipientPeerId: PeerIdHex,
    priority: Priority,
    payload: ByteArray,
    preferL2cap: Boolean,
    nowEpochMillis: Long,
  ): TransferEvent.Started {
    require(nowEpochMillis >= 0) {
      "TransferEngine nowEpochMillis must be greater than or equal to 0."
    }
    require(transferId !in sessionsByTransferId) {
      "TransferEngine already has an active transfer for $transferId."
    }

    val session =
      TransferSession(
        transferId = transferId,
        recipientPeerId = recipientPeerId,
        priority = priority,
        payload = payload,
        chunkSizeBytes = chunkSizePolicy.sizeFor(preferL2cap = preferL2cap),
        retransmitLimit = config.retransmitLimit,
      )
    sessionsByTransferId[transferId] =
      ManagedTransferSession(
        session = session,
        startedAtEpochMillis = nowEpochMillis,
        rateController = ObservationRateController(),
      )
    scheduler.enqueue(transferId = transferId, priority = priority)
    return TransferEvent.Started(transferId = transferId, priority = priority).also { event ->
      emitTransferStarted(event = event, totalBytes = payload.size.toLong())
    }
  }

  /** Returns the next transfer ID that should be serviced according to scheduler order. */
  public fun nextScheduledTransferId(): String? {
    return scheduler.dequeue()
  }

  /** Returns the next transmission window for the transfer. */
  public fun nextChunks(transferId: String): List<OutboundChunk> {
    val managedTransferSession: ManagedTransferSession =
      requireManagedTransferSession(transferId = transferId)
    val recommendedDelayMillis: Long =
      managedTransferSession.rateController.recommendedDelayMillis()
    val effectiveWindowSize: Int = if (recommendedDelayMillis > 0L) 1 else config.windowSize
    return managedTransferSession.session.nextChunks(windowSize = effectiveWindowSize)
  }

  /** Records acknowledgement progress for an outbound transfer. */
  public fun acknowledge(
    transferId: String,
    chunkIndex: Int,
    nowEpochMillis: Long,
  ): TransferEvent? {
    require(nowEpochMillis >= 0) {
      "TransferEngine nowEpochMillis must be greater than or equal to 0."
    }

    val managedTransferSession: ManagedTransferSession =
      sessionsByTransferId[transferId] ?: return null
    managedTransferSession.rateController.recordAcknowledgement(timestampMillis = nowEpochMillis)
    val event: TransferEvent = managedTransferSession.session.acknowledge(chunkIndex = chunkIndex)
    if (event is TransferEvent.Complete) {
      sessionsByTransferId.remove(transferId)
      emitTransferCompleted(event = event)
    } else if (event is TransferEvent.Progress) {
      emitTransferProgress(event = event)
    }
    return event
  }

  /** Cancels an active outbound transfer. */
  public fun cancel(transferId: String): TransferEvent.Failed? {
    val managedTransferSession: ManagedTransferSession =
      sessionsByTransferId.remove(transferId) ?: return null
    return managedTransferSession.session.cancel().also { event ->
      emitTransferFailed(event = event)
    }
  }

  /** Fails every transfer whose lifetime exceeded the configured timeout. */
  public fun failTimedOut(nowEpochMillis: Long): List<TransferEvent.Failed> {
    require(nowEpochMillis >= 0) {
      "TransferEngine nowEpochMillis must be greater than or equal to 0."
    }

    val timedOutTransferIds: List<String> =
      sessionsByTransferId.entries
        .filter { (_, managedTransferSession) ->
          nowEpochMillis - managedTransferSession.startedAtEpochMillis >= config.timeoutMillis
        }
        .map { (transferId, _) -> transferId }

    return timedOutTransferIds.map { transferId ->
      sessionsByTransferId.remove(transferId)!!
      TransferEvent.Failed(transferId = transferId, reason = FailureReason.TIMEOUT).also { event ->
        emitTransferFailed(event = event)
      }
    }
  }

  /**
   * Accepts an inbound chunk and returns the full payload once every expected chunk has arrived.
   */
  public fun receiveChunk(
    transferId: String,
    chunkIndex: Int,
    totalChunks: Int,
    payload: ByteArray,
  ): ByteArray? {
    require(totalChunks > 0) { "TransferEngine totalChunks must be greater than 0." }
    require(chunkIndex in 0 until totalChunks) {
      "TransferEngine chunkIndex must be between 0 and ${totalChunks - 1}."
    }

    inboundExpectedChunkCounts.getOrPut(transferId) { totalChunks }
    val inboundChunks: MutableMap<Int, ByteArray> =
      inboundChunksByTransferId.getOrPut(transferId) { linkedMapOf() }
    inboundChunks[chunkIndex] = payload.copyOf()

    return if (
      (0 until totalChunks).all { expectedChunkIndex -> expectedChunkIndex in inboundChunks }
    ) {
      val reassembledPayload: ByteArray =
        (0 until totalChunks)
          .flatMap { expectedChunkIndex -> inboundChunks.getValue(expectedChunkIndex).asList() }
          .toByteArray()
      inboundChunksByTransferId.remove(transferId)
      inboundExpectedChunkCounts.remove(transferId)
      reassembledPayload
    } else {
      null
    }
  }

  /** Returns the current pacing recommendation derived from recent acknowledgements. */
  public fun recommendedDelayMillis(transferId: String): Long? {
    val managedTransferSession: ManagedTransferSession =
      sessionsByTransferId[transferId] ?: return null
    return managedTransferSession.rateController.recommendedDelayMillis()
  }

  /** Returns how many outbound transfers are still active. */
  public fun pendingTransfers(): Int {
    return sessionsByTransferId.size
  }

  internal fun reset(): Unit {
    sessionsByTransferId.clear()
    inboundChunksByTransferId.clear()
    inboundExpectedChunkCounts.clear()
    scheduler.reset()
  }

  private fun requireManagedTransferSession(transferId: String): ManagedTransferSession {
    return requireNotNull(sessionsByTransferId[transferId]) {
      "TransferEngine has no active transfer for $transferId."
    }
  }

  private fun emitTransferStarted(event: TransferEvent.Started, totalBytes: Long): Unit {
    diagnosticSink.emit(code = DiagnosticCode.TRANSFER_STARTED) {
      DiagnosticPayload.TransferProgress(
        transferId = event.transferId,
        bytesTransferred = 0L,
        totalBytes = totalBytes,
      )
    }
  }

  private fun emitTransferProgress(event: TransferEvent.Progress): Unit {
    diagnosticSink.emit(code = DiagnosticCode.TRANSFER_PROGRESS) {
      DiagnosticPayload.TransferProgress(
        transferId = event.transferId,
        bytesTransferred = event.acknowledgedBytes,
        totalBytes = event.totalBytes,
      )
    }
  }

  private fun emitTransferCompleted(event: TransferEvent.Complete): Unit {
    diagnosticSink.emit(code = DiagnosticCode.TRANSFER_COMPLETED) {
      DiagnosticPayload.TransferProgress(
        transferId = event.transferId,
        bytesTransferred = event.totalBytes,
        totalBytes = event.totalBytes,
      )
    }
  }

  private fun emitTransferFailed(event: TransferEvent.Failed): Unit {
    diagnosticSink.emit(code = DiagnosticCode.TRANSFER_FAILED) {
      DiagnosticPayload.InternalError(message = "${event.transferId}:${event.reason.name}")
    }
  }
}

private data class ManagedTransferSession(
  val session: TransferSession,
  val startedAtEpochMillis: Long,
  val rateController: ObservationRateController,
)
