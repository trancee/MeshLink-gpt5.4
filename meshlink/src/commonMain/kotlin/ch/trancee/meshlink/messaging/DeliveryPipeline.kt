package ch.trancee.meshlink.messaging

import ch.trancee.meshlink.api.DiagnosticCode
import ch.trancee.meshlink.api.DiagnosticPayload
import ch.trancee.meshlink.api.DiagnosticSink
import ch.trancee.meshlink.api.NoOpDiagnosticSink

public class DeliveryPipeline(
    private val config: MessagingConfig,
    private val diagnosticSink: DiagnosticSink = NoOpDiagnosticSink,
    private val rateLimiter: SlidingWindowRateLimiter = SlidingWindowRateLimiter(
        windowMillis = config.rateLimitWindowMillis,
        maxMessagesPerWindow = config.maxMessagesPerWindow,
    ),
) {
    private val nextSequenceNumbersBySender: MutableMap<String, Long> = mutableMapOf()
    private val pendingDeliveries: MutableMap<MessageIdKey, PendingDelivery> = linkedMapOf()
    private val deliveredInboundMessageIds: MutableSet<MessageIdKey> = linkedSetOf()

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
                DiagnosticPayload.BufferPressure(
                    usedBytes = pendingDeliveries.size,
                    droppedEvents = 1,
                )
            }
            return SendResult.Rejected(reason = DeliveryFailureReason.BUFFER_PRESSURE)
        }

        val peerPair = PeerPair(senderPeerId = senderPeerId, recipientPeerId = recipientPeerId)
        if (!rateLimiter.tryAcquire(peerPair = peerPair, nowEpochMillis = nowEpochMillis)) {
            return SendResult.Queued(
                messageId = messageId,
                reason = QueuedReason.RATE_LIMITED,
            )
        }

        pendingDeliveries[messageId] = PendingDelivery(
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

    public fun receive(
        messageId: MessageIdKey,
        fromPeerId: ch.trancee.meshlink.api.PeerIdHex,
        payload: ByteArray,
    ): InboundMessage? {
        return if (deliveredInboundMessageIds.add(messageId)) {
            InboundMessage(
                messageId = messageId,
                fromPeerId = fromPeerId,
                payload = payload.copyOf(),
            )
        } else {
            null
        }
    }

    public fun acknowledge(messageId: MessageIdKey): DeliveryOutcome? {
        val pendingDelivery: PendingDelivery = pendingDeliveries.remove(messageId) ?: return null
        return Delivered(
            messageId = messageId,
            peerId = pendingDelivery.recipientPeerId,
        )
    }

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

    public fun failTimedOut(nowEpochMillis: Long): List<DeliveryOutcome> {
        require(nowEpochMillis >= 0) {
            "DeliveryPipeline nowEpochMillis must be greater than or equal to 0."
        }

        val timedOutMessageIds: List<MessageIdKey> = pendingDeliveries.entries
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
