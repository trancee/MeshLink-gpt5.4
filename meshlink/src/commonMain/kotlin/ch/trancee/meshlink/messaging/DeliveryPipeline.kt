package ch.trancee.meshlink.messaging

import ch.trancee.meshlink.api.DiagnosticCode
import ch.trancee.meshlink.api.DiagnosticPayload
import ch.trancee.meshlink.api.DiagnosticSink
import ch.trancee.meshlink.api.NoOpDiagnosticSink

/**
 * Coordinates outbound delivery bookkeeping, inbound deduplication, and cut-through relay support
 * for user-visible messages.
 */
public class DeliveryPipeline(
  private val config: MessagingConfig,
  private val diagnosticSink: DiagnosticSink = NoOpDiagnosticSink,
  private val rateLimiter: SlidingWindowRateLimiter =
    SlidingWindowRateLimiter(
      windowMillis = config.rateLimitWindowMillis,
      maxMessagesPerWindow = config.maxMessagesPerWindow,
    ),
  private val cutThroughBuffer: CutThroughBuffer = CutThroughBuffer(),
) {
  // Sequence numbers are tracked per sender so message IDs stay monotonic for each
  // originating peer without requiring a global counter.
  private val nextSequenceNumbersBySender: MutableMap<String, Long> = mutableMapOf()

  // Pending outbound deliveries are retained until they are acknowledged, cancelled,
  // or expired by the timeout sweep.
  private val pendingDeliveries: MutableMap<MessageIdKey, PendingDelivery> = linkedMapOf()

  // Inbound message IDs are remembered to provide at-most-once delivery semantics to
  // application code even if lower layers retransmit.
  private val deliveredInboundMessageIds: MutableSet<MessageIdKey> = linkedSetOf()

  /**
   * Registers a new outbound payload with the delivery pipeline.
   *
   * The payload is only considered sent once it survives capacity checks and the current rate limit
   * window.
   */
  public fun send(
    senderPeerId: ch.trancee.meshlink.api.PeerIdHex,
    recipientPeerId: ch.trancee.meshlink.api.PeerIdHex,
    payload: ByteArray,
    nowEpochMillis: Long,
  ): SendResult {
    require(nowEpochMillis >= 0) {
      "DeliveryPipeline nowEpochMillis must be greater than or equal to 0."
    }

    val messageId: MessageIdKey = nextMessageId(senderPeerId = senderPeerId)
    if (pendingDeliveries.size >= config.maxPendingMessages) {
      diagnosticSink.emit(code = DiagnosticCode.BUFFER_PRESSURE) {
        DiagnosticPayload.BufferPressure(usedBytes = pendingDeliveries.size, droppedEvents = 1)
      }
      return SendResult.Rejected(reason = DeliveryFailureReason.BUFFER_PRESSURE)
    }

    val peerPair = PeerPair(senderPeerId = senderPeerId, recipientPeerId = recipientPeerId)
    if (!rateLimiter.tryAcquire(peerPair = peerPair, nowEpochMillis = nowEpochMillis)) {
      return SendResult.Queued(messageId = messageId, reason = QueuedReason.RATE_LIMITED)
    }

    pendingDeliveries[messageId] =
      PendingDelivery(
        recipientPeerId = recipientPeerId,
        payload = payload.copyOf(),
        startedAtEpochMillis = nowEpochMillis,
      )
    diagnosticSink.emit(code = DiagnosticCode.MESSAGE_SENT) {
      DiagnosticPayload.PeerLifecycle(
        peerId = recipientPeerId,
        state = ch.trancee.meshlink.api.PeerState.Connected,
      )
    }
    return SendResult.Sent(messageId = messageId)
  }

  /** Accepts an inbound payload if it has not been delivered before. */
  public fun receive(
    messageId: MessageIdKey,
    fromPeerId: ch.trancee.meshlink.api.PeerIdHex,
    payload: ByteArray,
  ): InboundMessage? {
    return if (deliveredInboundMessageIds.add(messageId)) {
      InboundMessage(messageId = messageId, fromPeerId = fromPeerId, payload = payload.copyOf())
    } else {
      null
    }
  }

  /** Completes an outbound delivery once the remote side acknowledges it. */
  public fun acknowledge(messageId: MessageIdKey): DeliveryOutcome? {
    val pendingDelivery: PendingDelivery = pendingDeliveries.remove(messageId) ?: return null
    return Delivered(messageId = messageId, peerId = pendingDelivery.recipientPeerId)
  }

  /** Cancels an outbound delivery that should no longer be retried. */
  public fun cancel(messageId: MessageIdKey): DeliveryOutcome? {
    val pendingDelivery: PendingDelivery = pendingDeliveries.remove(messageId) ?: return null
    diagnosticSink.emit(code = DiagnosticCode.MESSAGE_FAILED) {
      DiagnosticPayload.InternalError(message = "cancelled")
    }
    return DeliveryOutcomeMapper.failed(
      messageId = messageId,
      peerId = pendingDelivery.recipientPeerId,
      reason = DeliveryFailureReason.CANCELLED,
    )
  }

  /** Expires every delivery that has exceeded the configured timeout window. */
  public fun failTimedOut(nowEpochMillis: Long): List<DeliveryOutcome> {
    require(nowEpochMillis >= 0) {
      "DeliveryPipeline nowEpochMillis must be greater than or equal to 0."
    }

    val timedOutMessageIds: List<MessageIdKey> =
      pendingDeliveries.entries
        .filter { (_, pendingDelivery) ->
          nowEpochMillis - pendingDelivery.startedAtEpochMillis >= config.deliveryTimeoutMillis
        }
        .map { (messageId, _) -> messageId }

    return timedOutMessageIds.map { messageId ->
      val pendingDelivery: PendingDelivery = pendingDeliveries.getValue(messageId)
      pendingDeliveries.remove(messageId)
      diagnosticSink.emit(code = DiagnosticCode.MESSAGE_FAILED) {
        DiagnosticPayload.InternalError(message = "timeout")
      }
      DeliveryOutcomeMapper.failed(
        messageId = messageId,
        peerId = pendingDelivery.recipientPeerId,
        reason = DeliveryFailureReason.TIMEOUT,
      )
    }
  }

  /**
   * Appends the local hop to an already streaming relay frame without fully re-buffering the
   * transfer payload.
   */
  public fun relayChunk0(chunk0: ByteArray, localHopPeerId: ByteArray): ByteArray {
    val forwardedFrame: ByteArray =
      cutThroughBuffer.appendVisitedHop(chunk0 = chunk0, hopPeerId = localHopPeerId)
    diagnosticSink.emit(code = DiagnosticCode.MESSAGE_SENT) {
      DiagnosticPayload.InternalError(message = "cut-through-relay")
    }
    return forwardedFrame
  }

  /** Number of outbound deliveries still awaiting a terminal outcome. */
  public fun pendingCount(): Int {
    return pendingDeliveries.size
  }

  private fun nextMessageId(senderPeerId: ch.trancee.meshlink.api.PeerIdHex): MessageIdKey {
    val nextSequenceNumber: Long = (nextSequenceNumbersBySender[senderPeerId.value] ?: 0L)
    nextSequenceNumbersBySender[senderPeerId.value] = nextSequenceNumber + 1L
    return MessageIdKey(senderPeerId = senderPeerId, sequenceNumber = nextSequenceNumber)
  }
}

private data class PendingDelivery(
  val recipientPeerId: ch.trancee.meshlink.api.PeerIdHex,
  val payload: ByteArray,
  val startedAtEpochMillis: Long,
)
