package ch.trancee.meshlink.wire.messages

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class BroadcastMessageCodecTest {
    @Test
    public fun encodeAndDecode_roundTripBroadcastMessage(): Unit {
        // Arrange
        val expectedOriginPeerId: ByteArray = byteArrayOf(0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C)
        val expectedSequenceNumber: Int = 99
        val expectedMaxHops: UByte = 3u
        val expectedPayload: ByteArray = byteArrayOf(0x21, 0x22)
        val message = BroadcastMessage(
            originPeerId = expectedOriginPeerId,
            sequenceNumber = expectedSequenceNumber,
            maxHops = expectedMaxHops,
            payload = expectedPayload,
        )

        // Act
        val encoded: ByteArray = BroadcastMessageCodec.encode(message = message)
        val decoded: BroadcastMessage = BroadcastMessageCodec.decode(payload = encoded)

        // Assert
        assertContentEquals(
            expected = expectedOriginPeerId,
            actual = decoded.originPeerId,
            message = "BroadcastMessageCodec should preserve the broadcast origin peer identifier",
        )
        assertEquals(
            expected = expectedSequenceNumber,
            actual = decoded.sequenceNumber,
            message = "BroadcastMessageCodec should preserve the broadcast sequence number",
        )
        assertEquals(
            expected = expectedMaxHops,
            actual = decoded.maxHops,
            message = "BroadcastMessageCodec should preserve maxHops",
        )
        assertContentEquals(
            expected = expectedPayload,
            actual = decoded.payload,
            message = "BroadcastMessageCodec should preserve the broadcast payload bytes",
        )
    }

    @Test
    public fun encode_throwsWhenOriginPeerIdLengthIsInvalid(): Unit {
        // Arrange
        val message = BroadcastMessage(
            originPeerId = byteArrayOf(0x01),
            sequenceNumber = 1,
            maxHops = 1u,
            payload = byteArrayOf(0x02),
        )

        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            BroadcastMessageCodec.encode(message = message)
        }

        // Assert
        assertEquals(
            expected = "Broadcast originPeerId must be exactly 12 bytes.",
            actual = error.message,
            message = "BroadcastMessageCodec should reject origin peer identifiers with the wrong length",
        )
    }

    @Test
    public fun decode_throwsWhenPayloadHeaderIsIncomplete(): Unit {
        // Arrange
        val payload: ByteArray = byteArrayOf(0x01)

        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            BroadcastMessageCodec.decode(payload = payload)
        }

        // Assert
        assertEquals(
            expected = "BROADCAST payload must contain originPeerId, sequenceNumber, and maxHops.",
            actual = error.message,
            message = "BroadcastMessageCodec should reject truncated BROADCAST headers",
        )
    }
}
