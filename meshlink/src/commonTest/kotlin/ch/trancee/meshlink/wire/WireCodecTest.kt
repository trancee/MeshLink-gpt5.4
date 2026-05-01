package ch.trancee.meshlink.wire

import ch.trancee.meshlink.wire.messages.HandshakeMessage
import ch.trancee.meshlink.wire.messages.HandshakeRound
import ch.trancee.meshlink.wire.messages.HandshakeMessageCodec
import ch.trancee.meshlink.wire.messages.HelloMessage
import ch.trancee.meshlink.wire.messages.HelloMessageCodec
import ch.trancee.meshlink.wire.messages.KeepaliveMessage
import ch.trancee.meshlink.wire.messages.KeepaliveMessageCodec
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
            MessageType.UPDATE.code.toByte(),
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
            expected = "WireCodec does not yet support decoding UPDATE messages.",
            actual = error.message,
            message = "WireCodec should surface unsupported message types until their specific codecs are implemented",
        )
    }

    private data object UnsupportedWireMessage : WireMessage
}
