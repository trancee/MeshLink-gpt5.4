package ch.trancee.meshlink.wire

import ch.trancee.meshlink.wire.messages.HelloMessage
import ch.trancee.meshlink.wire.messages.HelloMessageCodec
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
            MessageType.HANDSHAKE.code.toByte(),
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
            expected = "WireCodec does not yet support decoding HANDSHAKE messages.",
            actual = error.message,
            message = "WireCodec should surface unsupported message types until their specific codecs are implemented",
        )
    }

    private data object UnsupportedWireMessage : WireMessage
}
