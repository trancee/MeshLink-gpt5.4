package ch.trancee.meshlink.messaging

import ch.trancee.meshlink.api.DiagnosticCode
import ch.trancee.meshlink.api.DiagnosticPayload
import ch.trancee.meshlink.api.DiagnosticSink
import ch.trancee.meshlink.api.NoOpDiagnosticSink
import ch.trancee.meshlink.api.PeerIdHex
import ch.trancee.meshlink.api.PeerState

/**
 * Coordinates outbound delivery bookkeeping, inbound deduplication, cut-through relay support, and
 * bounded store-and-forward buffering for user-visible messages.
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
  private val nextSequenceNumbersBySender: MutableMap<String, Long> = mutableMapOf()
  private val pendingDeliveries: MutableMap<MessageIdKey, PendingDelivery> = linkedMapOf()
  private val bufferedDeliveries: MutableMap<MessageIdKey, BufferedDelivery> = linkedMapOf()
  private val deliveredInboundMessageIds: MutableSet<MessageIdKey> = linkedSetOf()

  public fun send(
    senderPeerId: PeerIdHex,
    recipientPeerId: PeerIdHex,
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
    emitMessageSent(recipientPeerId = recipientPeerId)
    return SendResult.Sent(messageId = messageId)
  }

  internal fun bufferForUnavailableRoute(
    senderPeerId: PeerIdHex,
    recipientPeerId: PeerIdHex,
    payload: ByteArray,
    nowEpochMillis: Long,
  ): SendResult {
    require(nowEpochMillis >= 0) {
      "DeliveryPipeline nowEpochMillis must be greater than or equal to 0."
    }

    if (bufferedDeliveries.size >= config.maxBufferedMessages) {
      val evictedMessageId: MessageIdKey = bufferedDeliveries.keys.first()
      bufferedDeliveries.remove(evictedMessageId)
      diagnosticSink.emit(code = DiagnosticCode.BUFFER_PRESSURE) {
        DiagnosticPayload.BufferPressure(usedBytes = config.maxBufferedMessages, droppedEvents = 1)
      }
    }

    val messageId: MessageIdKey = nextMessageId(senderPeerId = senderPeerId)
    bufferedDeliveries[messageId] =
      BufferedDelivery(
        senderPeerId = senderPeerId,
        recipientPeerId = recipientPeerId,
        payload = payload.copyOf(),
        bufferedAtEpochMillis = nowEpochMillis,
      )
    return SendResult.Queued(messageId = messageId, reason = QueuedReason.ROUTE_UNAVAILABLE)
  }

  internal fun flushBuffered(recipientPeerId: PeerIdHex, nowEpochMillis: Long): List<SendResult> {
    require(nowEpochMillis >= 0) {
      "DeliveryPipeline nowEpochMillis must be greater than or equal to 0."
    }

    val flushedResults: MutableList<SendResult> = mutableListOf()
    val candidateMessageIds: List<MessageIdKey> =
      bufferedDeliveries.keys.filter { messageId ->
        bufferedDeliveries.getValue(messageId).recipientPeerId == recipientPeerId
      }

    for (messageId in candidateMessageIds) {
      val bufferedDelivery: BufferedDelivery = bufferedDeliveries.getValue(messageId)
      val peerPair =
        PeerPair(
          senderPeerId = bufferedDelivery.senderPeerId,
          recipientPeerId = bufferedDelivery.recipientPeerId,
        )
      val canPromoteToPending: Boolean = pendingDeliveries.size < config.maxPendingMessages
      val allowedByRateLimiter: Boolean =
        canPromoteToPending &&
          rateLimiter.tryAcquire(peerPair = peerPair, nowEpochMillis = nowEpochMillis)
      if (!allowedByRateLimiter) {
        break
      }

      bufferedDeliveries.remove(messageId)
      pendingDeliveries[messageId] =
        PendingDelivery(
          recipientPeerId = bufferedDelivery.recipientPeerId,
          payload = bufferedDelivery.payload.copyOf(),
          startedAtEpochMillis = bufferedDelivery.bufferedAtEpochMillis,
        )
      emitMessageSent(recipientPeerId = bufferedDelivery.recipientPeerId)
      flushedResults += SendResult.Sent(messageId = messageId)
    }

    return flushedResults
  }

  public fun receive(
    messageId: MessageIdKey,
    fromPeerId: PeerIdHex,
    payload: ByteArray,
  ): InboundMessage? {
    return if (deliveredInboundMessageIds.add(messageId)) {
      InboundMessage(messageId = messageId, fromPeerId = fromPeerId, payload = payload.copyOf())
    } else {
      null
    }
  }

  public fun acknowledge(messageId: MessageIdKey): DeliveryOutcome? {
    val pendingDelivery: PendingDelivery? = pendingDeliveries.remove(messageId)
    if (pendingDelivery != null) {
      return Delivered(messageId = messageId, peerId = pendingDelivery.recipientPeerId)
    }

    val bufferedDelivery: BufferedDelivery? = bufferedDeliveries.remove(messageId)
    if (bufferedDelivery != null) {
      return Delivered(messageId = messageId, peerId = bufferedDelivery.recipientPeerId)
    }
    return null
  }

  public fun cancel(messageId: MessageIdKey): DeliveryOutcome? {
    val pendingDelivery: PendingDelivery? = pendingDeliveries.remove(messageId)
    if (pendingDelivery != null) {
      emitMessageFailed(reason = "cancelled")
      return DeliveryOutcomeMapper.failed(
        messageId = messageId,
        peerId = pendingDelivery.recipientPeerId,
        reason = DeliveryFailureReason.CANCELLED,
      )
    }

    val bufferedDelivery: BufferedDelivery? = bufferedDeliveries.remove(messageId)
    if (bufferedDelivery != null) {
      emitMessageFailed(reason = "cancelled")
      return DeliveryOutcomeMapper.failed(
        messageId = messageId,
        peerId = bufferedDelivery.recipientPeerId,
        reason = DeliveryFailureReason.CANCELLED,
      )
    }
    return null
  }

  public fun failTimedOut(nowEpochMillis: Long): List<DeliveryOutcome> {
    require(nowEpochMillis >= 0) {
      "DeliveryPipeline nowEpochMillis must be greater than or equal to 0."
    }

    val timedOutPendingMessageIds: List<MessageIdKey> =
      pendingDeliveries.entries
        .filter { (_, pendingDelivery) ->
          nowEpochMillis - pendingDelivery.startedAtEpochMillis >= config.deliveryTimeoutMillis
        }
        .map { (messageId, _) -> messageId }
    val timedOutBufferedMessageIds: List<MessageIdKey> =
      bufferedDeliveries.entries
        .filter { (_, bufferedDelivery) ->
          nowEpochMillis - bufferedDelivery.bufferedAtEpochMillis >= config.deliveryTimeoutMillis
        }
        .map { (messageId, _) -> messageId }

    val outcomes: MutableList<DeliveryOutcome> = mutableListOf()
    for (messageId in timedOutPendingMessageIds) {
      val pendingDelivery: PendingDelivery = pendingDeliveries.getValue(messageId)
      pendingDeliveries.remove(messageId)
      emitMessageFailed(reason = "timeout")
      outcomes +=
        DeliveryOutcomeMapper.failed(
          messageId = messageId,
          peerId = pendingDelivery.recipientPeerId,
          reason = DeliveryFailureReason.TIMEOUT,
        )
    }
    for (messageId in timedOutBufferedMessageIds) {
      val bufferedDelivery: BufferedDelivery = bufferedDeliveries.getValue(messageId)
      bufferedDeliveries.remove(messageId)
      emitMessageFailed(reason = "timeout")
      outcomes +=
        DeliveryOutcomeMapper.failed(
          messageId = messageId,
          peerId = bufferedDelivery.recipientPeerId,
          reason = DeliveryFailureReason.TIMEOUT,
        )
    }
    return outcomes
  }

  public fun relayChunk0(chunk0: ByteArray, localHopPeerId: ByteArray): ByteArray {
    val forwardedFrame: ByteArray =
      cutThroughBuffer.appendVisitedHop(chunk0 = chunk0, hopPeerId = localHopPeerId)
    diagnosticSink.emit(code = DiagnosticCode.MESSAGE_SENT) {
      DiagnosticPayload.InternalError(message = "cut-through-relay")
    }
    return forwardedFrame
  }

  public fun pendingCount(): Int {
    return pendingDeliveries.size
  }

  internal fun bufferedCount(): Int {
    return bufferedDeliveries.size
  }

  private fun nextMessageId(senderPeerId: PeerIdHex): MessageIdKey {
    val nextSequenceNumber: Long = nextSequenceNumbersBySender[senderPeerId.value] ?: 0L
    nextSequenceNumbersBySender[senderPeerId.value] = nextSequenceNumber + 1L
    return MessageIdKey(senderPeerId = senderPeerId, sequenceNumber = nextSequenceNumber)
  }

  private fun emitMessageSent(recipientPeerId: PeerIdHex): Unit {
    diagnosticSink.emit(code = DiagnosticCode.MESSAGE_SENT) {
      DiagnosticPayload.PeerLifecycle(peerId = recipientPeerId, state = PeerState.Connected)
    }
  }

  private fun emitMessageFailed(reason: String): Unit {
    diagnosticSink.emit(code = DiagnosticCode.MESSAGE_FAILED) {
      DiagnosticPayload.InternalError(message = reason)
    }
  }
}

private data class PendingDelivery(
  val recipientPeerId: PeerIdHex,
  val payload: ByteArray,
  val startedAtEpochMillis: Long,
)

private data class BufferedDelivery(
  val senderPeerId: PeerIdHex,
  val recipientPeerId: PeerIdHex,
  val payload: ByteArray,
  val bufferedAtEpochMillis: Long,
)
