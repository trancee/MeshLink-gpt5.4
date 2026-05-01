package ch.trancee.meshlink.messaging

import ch.trancee.meshlink.api.DiagnosticCode
import ch.trancee.meshlink.api.DiagnosticSink
import ch.trancee.meshlink.api.PeerIdHex
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

public class DeliveryPipelineTest {
    @Test
    public fun sendReceiveAndAcknowledge_completeTheCoreDeliveryCycle(): Unit {
        // Arrange
        val senderPeerId = PeerIdHex(value = "00112233")
        val recipientPeerId = PeerIdHex(value = "44556677")
        val pipeline = DeliveryPipeline(config = MessagingConfig.default())
        val payload = byteArrayOf(0x01, 0x02, 0x03)

        // Act
        val sendResult = pipeline.send(
            senderPeerId = senderPeerId,
            recipientPeerId = recipientPeerId,
            payload = payload,
            nowEpochMillis = 0L,
        )
        val messageId = assertIs<SendResult.Sent>(sendResult).messageId
        val inboundMessage = pipeline.receive(
            messageId = messageId,
            fromPeerId = senderPeerId,
            payload = payload,
        )
        val acknowledged = pipeline.acknowledge(messageId = messageId)

        // Assert
        assertIs<InboundMessage>(inboundMessage)
        assertContentEquals(expected = payload, actual = inboundMessage.payload)
        val delivered = assertIs<Delivered>(acknowledged)
        assertEquals(expected = messageId, actual = delivered.messageId)
        assertEquals(expected = recipientPeerId, actual = delivered.peerId)
        assertEquals(expected = 0, actual = pipeline.pendingCount())
    }

    @Test
    public fun receive_returnsNullForDuplicateInboundMessages(): Unit {
        // Arrange
        val senderPeerId = PeerIdHex(value = "00112233")
        val messageId = MessageIdKey(senderPeerId = senderPeerId, sequenceNumber = 1L)
        val pipeline = DeliveryPipeline(config = MessagingConfig.default())

        // Act
        pipeline.receive(messageId = messageId, fromPeerId = senderPeerId, payload = byteArrayOf(0x01))
        val duplicate = pipeline.receive(messageId = messageId, fromPeerId = senderPeerId, payload = byteArrayOf(0x01))

        // Assert
        assertNull(duplicate)
    }

    @Test
    public fun send_emitsMessageSentDiagnosticsForAcceptedDeliveries(): Unit {
        // Arrange
        val senderPeerId = PeerIdHex(value = "00112233")
        val recipientPeerId = PeerIdHex(value = "44556677")
        val diagnosticSink = DiagnosticSink.create(bufferSize = 4, clock = { 7L })
        val pipeline = DeliveryPipeline(
            config = MessagingConfig.default(),
            diagnosticSink = diagnosticSink,
        )

        // Act
        pipeline.send(
            senderPeerId = senderPeerId,
            recipientPeerId = recipientPeerId,
            payload = byteArrayOf(0x01),
            nowEpochMillis = 0L,
        )

        // Assert
        assertEquals(
            expected = listOf(DiagnosticCode.MESSAGE_SENT),
            actual = diagnosticSink.diagnosticEvents.replayCache.map { event -> event.code },
        )
    }
}
