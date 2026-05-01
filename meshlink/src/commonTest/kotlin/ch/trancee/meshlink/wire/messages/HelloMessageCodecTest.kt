package ch.trancee.meshlink.wire.messages

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class HelloMessageCodecTest {
    @Test
    public fun encodeAndDecode_roundTripHelloMessage(): Unit {
        // Arrange
        val expectedPeerId: ByteArray = byteArrayOf(0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B)
        val expectedAppIdHash: Int = 0x78563412
        val message = HelloMessage(
            peerId = expectedPeerId,
            appIdHash = expectedAppIdHash,
        )

        // Act
        val encoded: ByteArray = HelloMessageCodec.encode(message = message)
        val decoded: HelloMessage = HelloMessageCodec.decode(payload = encoded)

        // Assert
        assertContentEquals(
            expected = expectedPeerId,
            actual = decoded.peerId,
            message = "HelloMessageCodec should preserve the peer identifier bytes",
        )
        assertEquals(
            expected = expectedAppIdHash,
            actual = decoded.appIdHash,
            message = "HelloMessageCodec should preserve the appIdHash",
        )
    }

    @Test
    public fun encode_throwsWhenPeerIdLengthIsInvalid(): Unit {
        // Arrange
        val message = HelloMessage(
            peerId = byteArrayOf(0x01, 0x02),
            appIdHash = 1,
        )

        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            HelloMessageCodec.encode(message = message)
        }

        // Assert
        assertEquals(
            expected = "Hello peerId must be exactly 12 bytes.",
            actual = error.message,
            message = "HelloMessageCodec should reject peer identifiers with the wrong length",
        )
    }

    @Test
    public fun decode_throwsWhenPayloadLengthIsInvalid(): Unit {
        // Arrange
        val payload: ByteArray = byteArrayOf(0x01, 0x02, 0x03)

        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            HelloMessageCodec.decode(payload = payload)
        }

        // Assert
        assertEquals(
            expected = "HELLO payload must be exactly 16 bytes.",
            actual = error.message,
            message = "HelloMessageCodec should reject malformed HELLO payload sizes",
        )
    }
}
