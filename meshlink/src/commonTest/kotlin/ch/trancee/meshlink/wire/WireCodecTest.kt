package ch.trancee.meshlink.wire

import ch.trancee.meshlink.wire.messages.DeliveryAckMessage
import ch.trancee.meshlink.wire.messages.DeliveryAckMessageCodec
import ch.trancee.meshlink.wire.messages.HandshakeMessage
import ch.trancee.meshlink.wire.messages.HandshakeRound
import ch.trancee.meshlink.wire.messages.HandshakeMessageCodec
import ch.trancee.meshlink.wire.messages.HelloMessage
import ch.trancee.meshlink.wire.messages.HelloMessageCodec
import ch.trancee.meshlink.wire.messages.KeepaliveMessage
import ch.trancee.meshlink.wire.messages.KeepaliveMessageCodec
import ch.trancee.meshlink.wire.messages.NackMessage
import ch.trancee.meshlink.wire.messages.NackMessageCodec
import ch.trancee.meshlink.wire.messages.ResumeRequestMessage
import ch.trancee.meshlink.wire.messages.ResumeRequestMessageCodec
import ch.trancee.meshlink.wire.messages.RoutedMessage
import ch.trancee.meshlink.wire.messages.RoutedMessageCodec
import ch.trancee.meshlink.wire.messages.UpdateMessage
import ch.trancee.meshlink.wire.messages.UpdateMessageCodec
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

public class WireCodecTest {
    @Test
    public fun encodeAndDecode_roundTripHelloMessageThroughDispatcher(): Unit {
        // Arrange
        val expectedPeerId: ByteArray = byteArrayOf(0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C)
        val expectedAppIdHash: Int = 0x11223344
        val message = HelloMessage(
            peerId = expectedPeerId,
            appIdHash = expectedAppIdHash,
        )

        // Act
        val decoded: HelloMessage = assertIs<HelloMessage>(WireCodec.decode(encoded = WireCodec.encode(message = message)))

        // Assert
        assertContentEquals(
            expected = expectedPeerId,
            actual = decoded.peerId,
            message = "WireCodec should preserve the HELLO peer identifier through encode/decode dispatch",
        )
        assertEquals(
            expected = expectedAppIdHash,
            actual = decoded.appIdHash,
            message = "WireCodec should preserve the HELLO appIdHash through encode/decode dispatch",
        )
    }

    @Test
    public fun encode_writesMessageTypeAndLittleEndianPayloadLength(): Unit {
        // Arrange
        val message = HelloMessage(
            peerId = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C),
            appIdHash = 0x78563412,
        )
        val expectedPayload: ByteArray = HelloMessageCodec.encode(message = message)

        // Act
        val encoded: ByteArray = WireCodec.encode(message = message)

        // Assert
        assertContentEquals(
            expected = byteArrayOf(MessageType.HELLO.code.toByte(), 0x10, 0x00, 0x00, 0x00) + expectedPayload,
            actual = encoded,
            message = "WireCodec should write the one-byte message type and little-endian payload length before the payload",
        )
    }

    @Test
    public fun encodeAndDecode_roundTripHandshakeMessageThroughDispatcher(): Unit {
        // Arrange
        val expectedPayload: ByteArray = byteArrayOf(0x51, 0x52, 0x53)
        val message = HandshakeMessage(
            round = HandshakeRound.ONE,
            payload = expectedPayload,
        )

        // Act
        val decoded: HandshakeMessage = assertIs<HandshakeMessage>(WireCodec.decode(encoded = WireCodec.encode(message = message)))

        // Assert
        assertEquals(
            expected = HandshakeRound.ONE,
            actual = decoded.round,
            message = "WireCodec should preserve the handshake round through encode/decode dispatch",
        )
        assertContentEquals(
            expected = expectedPayload,
            actual = decoded.payload,
            message = "WireCodec should preserve the handshake payload through encode/decode dispatch",
        )
    }

    @Test
    public fun encode_writesHandshakeTypeAndPayloadLength(): Unit {
        // Arrange
        val message = HandshakeMessage(
            round = HandshakeRound.THREE,
            payload = byteArrayOf(0x61, 0x62),
        )
        val expectedPayload: ByteArray = HandshakeMessageCodec.encode(message = message)

        // Act
        val encoded: ByteArray = WireCodec.encode(message = message)

        // Assert
        assertContentEquals(
            expected = byteArrayOf(MessageType.HANDSHAKE.code.toByte(), 0x03, 0x00, 0x00, 0x00) + expectedPayload,
            actual = encoded,
            message = "WireCodec should frame HANDSHAKE messages with the correct type tag and payload length",
        )
    }

    @Test
    public fun encodeAndDecode_roundTripKeepaliveMessageThroughDispatcher(): Unit {
        // Arrange
        val message = KeepaliveMessage

        // Act
        val decoded: KeepaliveMessage = assertIs<KeepaliveMessage>(WireCodec.decode(encoded = WireCodec.encode(message = message)))

        // Assert
        assertEquals(
            expected = KeepaliveMessage,
            actual = decoded,
            message = "WireCodec should preserve KEEPALIVE through encode/decode dispatch",
        )
    }

    @Test
    public fun encode_writesKeepaliveTypeAndZeroLengthPayload(): Unit {
        // Arrange
        val expectedPayload: ByteArray = KeepaliveMessageCodec.encode()

        // Act
        val encoded: ByteArray = WireCodec.encode(message = KeepaliveMessage)

        // Assert
        assertContentEquals(
            expected = byteArrayOf(MessageType.KEEPALIVE.code.toByte(), 0x00, 0x00, 0x00, 0x00) + expectedPayload,
            actual = encoded,
            message = "WireCodec should frame KEEPALIVE messages with a zero-length payload",
        )
    }

    @Test
    public fun encodeAndDecode_roundTripRoutedMessageThroughDispatcher(): Unit {
        // Arrange
        val expectedPayload: ByteArray = byteArrayOf(0x71, 0x72)
        val message = RoutedMessage(
            hopCount = 1u,
            maxHops = 4u,
            payload = expectedPayload,
        )

        // Act
        val decoded: RoutedMessage = assertIs<RoutedMessage>(WireCodec.decode(encoded = WireCodec.encode(message = message)))

        // Assert
        assertEquals(
            expected = 1u,
            actual = decoded.hopCount,
            message = "WireCodec should preserve routed hopCount through encode/decode dispatch",
        )
        assertEquals(
            expected = 4u,
            actual = decoded.maxHops,
            message = "WireCodec should preserve routed maxHops through encode/decode dispatch",
        )
        assertContentEquals(
            expected = expectedPayload,
            actual = decoded.payload,
            message = "WireCodec should preserve routed payload bytes through encode/decode dispatch",
        )
    }

    @Test
    public fun encode_writesRoutedMessageTypeAndPayloadLength(): Unit {
        // Arrange
        val message = RoutedMessage(
            hopCount = 2u,
            maxHops = 5u,
            payload = byteArrayOf(0x21, 0x22, 0x23),
        )
        val expectedPayload: ByteArray = RoutedMessageCodec.encode(message = message)

        // Act
        val encoded: ByteArray = WireCodec.encode(message = message)

        // Assert
        assertContentEquals(
            expected = byteArrayOf(MessageType.ROUTED_MESSAGE.code.toByte(), 0x05, 0x00, 0x00, 0x00) + expectedPayload,
            actual = encoded,
            message = "WireCodec should frame ROUTED_MESSAGE messages with the correct type tag and payload length",
        )
    }

    @Test
    public fun encodeAndDecode_roundTripUpdateMessageThroughDispatcher(): Unit {
        // Arrange
        val expectedDestinationPeerId: ByteArray = byteArrayOf(0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C)
        val message = UpdateMessage(
            destinationPeerId = expectedDestinationPeerId,
            metric = 123,
            seqno = 7,
        )

        // Act
        val decoded: UpdateMessage = assertIs<UpdateMessage>(WireCodec.decode(encoded = WireCodec.encode(message = message)))

        // Assert
        assertContentEquals(
            expected = expectedDestinationPeerId,
            actual = decoded.destinationPeerId,
            message = "WireCodec should preserve the UPDATE destination peer identifier through encode/decode dispatch",
        )
        assertEquals(
            expected = 123,
            actual = decoded.metric,
            message = "WireCodec should preserve the UPDATE metric through encode/decode dispatch",
        )
        assertEquals(
            expected = 7,
            actual = decoded.seqno,
            message = "WireCodec should preserve the UPDATE sequence number through encode/decode dispatch",
        )
    }

    @Test
    public fun encode_writesUpdateTypeAndPayloadLength(): Unit {
        // Arrange
        val message = UpdateMessage(
            destinationPeerId = byteArrayOf(0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4A, 0x4B, 0x4C),
            metric = 99,
            seqno = 5,
        )
        val expectedPayload: ByteArray = UpdateMessageCodec.encode(message = message)

        // Act
        val encoded: ByteArray = WireCodec.encode(message = message)

        // Assert
        assertContentEquals(
            expected = byteArrayOf(MessageType.UPDATE.code.toByte(), 0x14, 0x00, 0x00, 0x00) + expectedPayload,
            actual = encoded,
            message = "WireCodec should frame UPDATE messages with the correct type tag and payload length",
        )
    }

    @Test
    public fun encodeAndDecode_roundTripDeliveryAckMessageThroughDispatcher(): Unit {
        // Arrange
        val message = DeliveryAckMessage(messageId = 0x8877665544332211uL.toLong())

        // Act
        val decoded: DeliveryAckMessage = assertIs<DeliveryAckMessage>(WireCodec.decode(encoded = WireCodec.encode(message = message)))

        // Assert
        assertEquals(
            expected = 0x8877665544332211uL.toLong(),
            actual = decoded.messageId,
            message = "WireCodec should preserve the DELIVERY_ACK message identifier through encode/decode dispatch",
        )
    }

    @Test
    public fun encode_writesDeliveryAckTypeAndPayloadLength(): Unit {
        // Arrange
        val message = DeliveryAckMessage(messageId = 0x0102030405060708)
        val expectedPayload: ByteArray = DeliveryAckMessageCodec.encode(message = message)

        // Act
        val encoded: ByteArray = WireCodec.encode(message = message)

        // Assert
        assertContentEquals(
            expected = byteArrayOf(MessageType.DELIVERY_ACK.code.toByte(), 0x08, 0x00, 0x00, 0x00) + expectedPayload,
            actual = encoded,
            message = "WireCodec should frame DELIVERY_ACK messages with the correct type tag and payload length",
        )
    }

    @Test
    public fun encodeAndDecode_roundTripNackMessageThroughDispatcher(): Unit {
        // Arrange
        val message = NackMessage(
            messageId = 0x1020304050607080,
            reasonCode = 503,
        )

        // Act
        val decoded: NackMessage = assertIs<NackMessage>(WireCodec.decode(encoded = WireCodec.encode(message = message)))

        // Assert
        assertEquals(
            expected = 0x1020304050607080,
            actual = decoded.messageId,
            message = "WireCodec should preserve the NACK message identifier through encode/decode dispatch",
        )
        assertEquals(
            expected = 503,
            actual = decoded.reasonCode,
            message = "WireCodec should preserve the NACK reason code through encode/decode dispatch",
        )
    }

    @Test
    public fun encode_writesNackTypeAndPayloadLength(): Unit {
        // Arrange
        val message = NackMessage(
            messageId = 0x0102030405060708,
            reasonCode = 404,
        )
        val expectedPayload: ByteArray = NackMessageCodec.encode(message = message)

        // Act
        val encoded: ByteArray = WireCodec.encode(message = message)

        // Assert
        assertContentEquals(
            expected = byteArrayOf(MessageType.NACK.code.toByte(), 0x0C, 0x00, 0x00, 0x00) + expectedPayload,
            actual = encoded,
            message = "WireCodec should frame NACK messages with the correct type tag and payload length",
        )
    }

    @Test
    public fun encodeAndDecode_roundTripResumeRequestMessageThroughDispatcher(): Unit {
        // Arrange
        val message = ResumeRequestMessage(
            transferId = 0x0102030405060708,
            resumeOffset = 0x1112131415161718,
        )

        // Act
        val decoded: ResumeRequestMessage = assertIs<ResumeRequestMessage>(WireCodec.decode(encoded = WireCodec.encode(message = message)))

        // Assert
        assertEquals(
            expected = 0x0102030405060708,
            actual = decoded.transferId,
            message = "WireCodec should preserve the RESUME_REQUEST transfer identifier through encode/decode dispatch",
        )
        assertEquals(
            expected = 0x1112131415161718,
            actual = decoded.resumeOffset,
            message = "WireCodec should preserve the RESUME_REQUEST offset through encode/decode dispatch",
        )
    }

    @Test
    public fun encode_writesResumeRequestTypeAndPayloadLength(): Unit {
        // Arrange
        val message = ResumeRequestMessage(
            transferId = 0x2122232425262728,
            resumeOffset = 0x3132333435363738,
        )
        val expectedPayload: ByteArray = ResumeRequestMessageCodec.encode(message = message)

        // Act
        val encoded: ByteArray = WireCodec.encode(message = message)

        // Assert
        assertContentEquals(
            expected = byteArrayOf(MessageType.RESUME_REQUEST.code.toByte(), 0x10, 0x00, 0x00, 0x00) + expectedPayload,
            actual = encoded,
            message = "WireCodec should frame RESUME_REQUEST messages with the correct type tag and payload length",
        )
    }

    @Test
    public fun encode_throwsWhenMessageImplementationIsNotYetSupported(): Unit {
        // Arrange
        val message: WireMessage = UnsupportedWireMessage

        // Act
        val error = assertFailsWith<UnsupportedOperationException> {
            WireCodec.encode(message = message)
        }

        // Assert
        assertEquals(
            expected = "WireCodec does not yet support encoding UnsupportedWireMessage messages.",
            actual = error.message,
            message = "WireCodec should surface unsupported message implementations until their specific codecs are implemented",
        )
    }

    @Test
    public fun decode_throwsWhenFrameIsShorterThanHeader(): Unit {
        // Arrange
        val encoded: ByteArray = byteArrayOf(MessageType.HELLO.code.toByte(), 0x00, 0x00, 0x00)

        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            WireCodec.decode(encoded = encoded)
        }

        // Assert
        assertEquals(
            expected = "Encoded frame is shorter than the 5-byte header.",
            actual = error.message,
            message = "WireCodec should reject encoded frames that cannot contain a full header",
        )
    }

    @Test
    public fun decode_throwsWhenDeclaredPayloadLengthDoesNotMatchActualLength(): Unit {
        // Arrange
        val encoded: ByteArray = byteArrayOf(
            MessageType.HELLO.code.toByte(),
            0x02,
            0x00,
            0x00,
            0x00,
            0x0A,
        )

        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            WireCodec.decode(encoded = encoded)
        }

        // Assert
        assertEquals(
            expected = "Encoded frame length mismatch: expected 7 bytes but received 6.",
            actual = error.message,
            message = "WireCodec should reject frames whose declared payload length differs from the actual byte count",
        )
    }

    @Test
    public fun decode_throwsWhenDeclaredPayloadLengthIsNegative(): Unit {
        // Arrange
        val encoded: ByteArray = byteArrayOf(
            MessageType.HELLO.code.toByte(),
            0xFF.toByte(),
            0xFF.toByte(),
            0xFF.toByte(),
            0xFF.toByte(),
        )

        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            WireCodec.decode(encoded = encoded)
        }

        // Assert
        assertEquals(
            expected = "Encoded frame declares a negative payload length.",
            actual = error.message,
            message = "WireCodec should reject frames whose payload length header decodes to a negative value",
        )
    }

    @Test
    public fun decode_throwsWhenMessageTypeIsRecognizedButCodecIsNotYetImplemented(): Unit {
        // Arrange
        val encoded: ByteArray = byteArrayOf(
            MessageType.BROADCAST.code.toByte(),
            0x00,
            0x00,
            0x00,
            0x00,
        )

        // Act
        val error = assertFailsWith<UnsupportedOperationException> {
            WireCodec.decode(encoded = encoded)
        }

        // Assert
        assertEquals(
            expected = "WireCodec does not yet support decoding BROADCAST messages.",
            actual = error.message,
            message = "WireCodec should surface unsupported message types until their specific codecs are implemented",
        )
    }

    private data object UnsupportedWireMessage : WireMessage
}
