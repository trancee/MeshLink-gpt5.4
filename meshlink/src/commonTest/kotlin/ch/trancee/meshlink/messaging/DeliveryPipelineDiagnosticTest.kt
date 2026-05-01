package ch.trancee.meshlink.messaging

import ch.trancee.meshlink.api.DiagnosticCode
import ch.trancee.meshlink.api.DiagnosticPayload
import ch.trancee.meshlink.api.DiagnosticSink
import ch.trancee.meshlink.api.PeerIdHex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

public class DeliveryPipelineDiagnosticTest {
    @Test
    public fun send_emitsBufferPressureDiagnosticsWhenPendingCapacityIsExceeded(): Unit {
        // Arrange
        val senderPeerId = PeerIdHex(value = "00112233")
        val recipientPeerId = PeerIdHex(value = "44556677")
        val diagnosticSink = DiagnosticSink.create(bufferSize = 8, clock = { 10L })
        val pipeline = DeliveryPipeline(
            config = MessagingConfig(
                rateLimitWindowMillis = 1_000L,
                maxMessagesPerWindow = 10,
                deliveryTimeoutMillis = 5_000L,
                maxPendingMessages = 1,
                appIdHash = 0,
            ),
            diagnosticSink = diagnosticSink,
        )
        pipeline.send(senderPeerId = senderPeerId, recipientPeerId = recipientPeerId, payload = byteArrayOf(0x01), nowEpochMillis = 0L)

        // Act
        val actual = pipeline.send(
            senderPeerId = senderPeerId,
            recipientPeerId = recipientPeerId,
            payload = byteArrayOf(0x02),
            nowEpochMillis = 1L,
        )

        // Assert
        assertEquals(expected = SendResult.Rejected(reason = DeliveryFailureReason.BUFFER_PRESSURE), actual = actual)
        assertEquals(
            expected = listOf(DiagnosticCode.MESSAGE_SENT, DiagnosticCode.BUFFER_PRESSURE),
            actual = diagnosticSink.diagnosticEvents.replayCache.map { event -> event.code },
        )
        val payload = assertIs<DiagnosticPayload.BufferPressure>(diagnosticSink.diagnosticEvents.replayCache.last().payload)
        assertEquals(expected = 1, actual = payload.usedBytes)
        assertEquals(expected = 1, actual = payload.droppedEvents)
    }

    @Test
    public fun cancelAndTimeout_emitMessageFailedDiagnostics(): Unit {
        // Arrange
        val senderPeerId = PeerIdHex(value = "00112233")
        val recipientPeerId = PeerIdHex(value = "44556677")
        val diagnosticSink = DiagnosticSink.create(bufferSize = 8, clock = { 20L })
        val pipeline = DeliveryPipeline(
            config = MessagingConfig.default(),
            diagnosticSink = diagnosticSink,
        )
        val cancelMessageId = assertIs<SendResult.Sent>(
            pipeline.send(senderPeerId = senderPeerId, recipientPeerId = recipientPeerId, payload = byteArrayOf(0x01), nowEpochMillis = 0L),
        ).messageId
        val timeoutMessageId = assertIs<SendResult.Sent>(
            pipeline.send(senderPeerId = senderPeerId, recipientPeerId = recipientPeerId, payload = byteArrayOf(0x02), nowEpochMillis = 1L),
        ).messageId

        // Act
        pipeline.cancel(messageId = cancelMessageId)
        pipeline.failTimedOut(nowEpochMillis = MessagingConfig.default().deliveryTimeoutMillis + 1L)
        val actualMessages = diagnosticSink.diagnosticEvents.replayCache.map { event ->
            val payload = event.payload as? DiagnosticPayload.InternalError
            event.code to payload?.message
        }

        // Assert
        assertEquals(
            expected = listOf(
                DiagnosticCode.MESSAGE_SENT to null,
                DiagnosticCode.MESSAGE_SENT to null,
                DiagnosticCode.MESSAGE_FAILED to "cancelled",
                DiagnosticCode.MESSAGE_FAILED to "timeout",
            ),
            actual = actualMessages,
        )
        assertEquals(expected = timeoutMessageId, actual = timeoutMessageId)
    }
}
