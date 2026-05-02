package ch.trancee.meshlink.transfer

import ch.trancee.meshlink.api.PeerIdHex

/** Outbound transfer state for a single payload. */
public class TransferSession(
  public val transferId: String,
  public val recipientPeerId: PeerIdHex,
  public val priority: Priority,
  private val payload: ByteArray,
  private val chunkSizeBytes: Int,
  private val retransmitLimit: Int = Int.MAX_VALUE,
) {
  private val sackTracker: SackTracker
  private val transmissionAttemptsByChunkIndex: MutableMap<Int, Int> = mutableMapOf()

  init {
    require(transferId.isNotBlank()) { "TransferSession transferId must not be blank." }
    require(payload.isNotEmpty()) { "TransferSession payload must not be empty." }
    require(chunkSizeBytes > 0) { "TransferSession chunkSizeBytes must be greater than 0." }
    require(retransmitLimit >= 0) {
      "TransferSession retransmitLimit must be greater than or equal to 0."
    }
    sackTracker = SackTracker(totalChunks = totalChunkCount(payloadSize = payload.size))
  }

  public fun totalChunks(): Int {
    return sackTracker.totalChunks
  }

  /** Returns the next window of missing chunks that should be transmitted. */
  public fun nextChunks(windowSize: Int): List<OutboundChunk> {
    require(windowSize > 0) { "TransferSession windowSize must be greater than 0." }

    val eligibleChunkIndices: List<Int> =
      sackTracker.missingChunks().filter { chunkIndex ->
        (transmissionAttemptsByChunkIndex[chunkIndex] ?: 0) <= retransmitLimit
      }
    val selectedChunkIndices: List<Int> = eligibleChunkIndices.take(n = windowSize)
    selectedChunkIndices.forEach { chunkIndex ->
      val previousAttempts: Int = transmissionAttemptsByChunkIndex[chunkIndex] ?: 0
      transmissionAttemptsByChunkIndex[chunkIndex] = previousAttempts + 1
    }
    return selectedChunkIndices.map { chunkIndex ->
      OutboundChunk(
        transferId = transferId,
        chunkIndex = chunkIndex,
        payload = chunkPayload(chunkIndex = chunkIndex),
      )
    }
  }

  /** Records acknowledgement of a chunk and emits progress or completion. */
  public fun acknowledge(chunkIndex: Int): TransferEvent {
    sackTracker.acknowledge(chunkIndex = chunkIndex)
    val acknowledgedBytes: Long =
      sackTracker.acknowledgedChunks().sumOf { acknowledgedChunkIndex ->
        chunkPayload(chunkIndex = acknowledgedChunkIndex).size.toLong()
      }

    return if (sackTracker.isComplete()) {
      TransferEvent.Complete(transferId = transferId, totalBytes = payload.size.toLong())
    } else {
      TransferEvent.Progress(
        transferId = transferId,
        acknowledgedBytes = acknowledgedBytes,
        totalBytes = payload.size.toLong(),
      )
    }
  }

  /** Returns the byte offset from which a resumed transfer should continue. */
  public fun resumeOffsetBytes(): Int {
    return ResumeCalculator.resumeOffsetBytes(
      sackTracker = sackTracker,
      chunkSizeBytes = chunkSizeBytes,
      totalBytes = payload.size,
    )
  }

  internal fun isRetransmitBudgetExhausted(): Boolean {
    return !sackTracker.isComplete() &&
      sackTracker.missingChunks().all { chunkIndex ->
        (transmissionAttemptsByChunkIndex[chunkIndex] ?: 0) > retransmitLimit
      }
  }

  public fun cancel(): TransferEvent.Failed {
    return TransferEvent.Failed(transferId = transferId, reason = FailureReason.CANCELLED)
  }

  public fun isComplete(): Boolean {
    return sackTracker.isComplete()
  }

  private fun chunkPayload(chunkIndex: Int): ByteArray {
    val startIndex: Int = chunkIndex * chunkSizeBytes
    val endIndex: Int = minOf(a = startIndex + chunkSizeBytes, b = payload.size)
    return payload.copyOfRange(fromIndex = startIndex, toIndex = endIndex)
  }

  private fun totalChunkCount(payloadSize: Int): Int {
    return (payloadSize + chunkSizeBytes - 1) / chunkSizeBytes
  }
}

/** Wire-ready outbound transfer chunk. */
public data class OutboundChunk(
  public val transferId: String,
  public val chunkIndex: Int,
  public val payload: ByteArray,
)
