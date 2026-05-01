package ch.trancee.meshlink.transport

import ch.trancee.meshlink.wire.WriteBuffer
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

public class L2capFrameCodecTest {
    @Test
    public fun defaultMaxFrameLength_isSixtyFourKilobytes(): Unit {
        // Arrange
        val expected = 64 * 1024

        // Act
        val actual = L2capFrameCodec.DEFAULT_MAX_FRAME_LENGTH_BYTES

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    public fun encode_prefixesPayloadWithLittleEndianLength(): Unit {
        // Arrange
        val codec = L2capFrameCodec()
        val payload = byteArrayOf(0x01, 0x02, 0x03)

        // Act
        val actual = codec.encode(payload = payload)

        // Assert
        assertContentEquals(
            expected = byteArrayOf(0x03, 0x00, 0x00, 0x00, 0x01, 0x02, 0x03),
            actual = actual,
            message = "L2capFrameCodec should prefix frames with a 32-bit little-endian payload length",
        )
    }

    @Test
    public fun append_decodesACompleteFrame(): Unit {
        // Arrange
        val codec = L2capFrameCodec()
        val expected = byteArrayOf(0x0A, 0x0B)
        val encoded = codec.encode(payload = expected)

        // Act
        val actual = codec.append(chunk = encoded)

        // Assert
        assertEquals(expected = 1, actual = actual.size)
        assertContentEquals(expected = expected, actual = actual.single())
    }

    @Test
    public fun append_buffersFragmentedHeadersAndPayloadsUntilComplete(): Unit {
        // Arrange
        val codec = L2capFrameCodec()
        val expected = byteArrayOf(0x11, 0x12, 0x13)
        val encoded = codec.encode(payload = expected)

        // Act
        val first = codec.append(chunk = encoded.copyOfRange(fromIndex = 0, toIndex = 2))
        val second = codec.append(chunk = encoded.copyOfRange(fromIndex = 2, toIndex = 5))
        val third = codec.append(chunk = encoded.copyOfRange(fromIndex = 5, toIndex = encoded.size))

        // Assert
        assertTrue(actual = first.isEmpty())
        assertTrue(actual = second.isEmpty())
        assertEquals(expected = 1, actual = third.size)
        assertContentEquals(expected = expected, actual = third.single())
    }

    @Test
    public fun append_decodesMultipleFramesFromOneChunk(): Unit {
        // Arrange
        val codec = L2capFrameCodec()
        val firstPayload = byteArrayOf(0x21)
        val secondPayload = byteArrayOf(0x22, 0x23)
        val chunk = codec.encode(payload = firstPayload) + codec.encode(payload = secondPayload)

        // Act
        val actual = codec.append(chunk = chunk)

        // Assert
        assertEquals(expected = 2, actual = actual.size)
        assertContentEquals(expected = firstPayload, actual = actual[0])
        assertContentEquals(expected = secondPayload, actual = actual[1])
    }

    @Test
    public fun append_allowsAnEmptyChunkWhenBufferedDataExists(): Unit {
        // Arrange
        val codec = L2capFrameCodec()
        val encoded = codec.encode(payload = byteArrayOf(0x33, 0x34))
        codec.append(chunk = encoded.copyOfRange(fromIndex = 0, toIndex = 2))

        // Act
        val actual = codec.append(chunk = byteArrayOf())

        // Assert
        assertTrue(
            actual = actual.isEmpty(),
            message = "L2capFrameCodec should allow empty chunks while waiting for the remainder of a buffered frame",
        )
    }

    @Test
    public fun append_decodesZeroLengthFrames(): Unit {
        // Arrange
        val codec = L2capFrameCodec()
        val encoded = codec.encode(payload = byteArrayOf())

        // Act
        val actual = codec.append(chunk = encoded)

        // Assert
        assertEquals(expected = 1, actual.size)
        assertContentEquals(expected = byteArrayOf(), actual = actual.single())
    }

    @Test
    public fun append_rejectsAnEmptyChunkWhenNothingIsBuffered(): Unit {
        // Arrange
        val codec = L2capFrameCodec()

        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            codec.append(chunk = byteArrayOf())
        }

        // Assert
        assertEquals(
            expected = "L2capFrameCodec append requires either a non-empty chunk or buffered frame data.",
            actual = error.message,
        )
    }

    @Test
    public fun encode_rejectsPayloadsLargerThanTheConfiguredMaximum(): Unit {
        // Arrange
        val codec = L2capFrameCodec(maxFrameLengthBytes = 1)

        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            codec.encode(payload = byteArrayOf(0x01, 0x02))
        }

        // Assert
        assertEquals(
            expected = "L2capFrameCodec frame length must be between 0 and 1 bytes.",
            actual = error.message,
        )
    }

    @Test
    public fun append_rejectsDecodedLengthsOutsideTheConfiguredMaximum(): Unit {
        // Arrange
        val codec = L2capFrameCodec(maxFrameLengthBytes = 4)
        val encoded = WriteBuffer().apply {
            writeInt(value = 5)
            writeBytes(value = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05))
        }.toByteArray()

        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            codec.append(chunk = encoded)
        }

        // Assert
        assertEquals(
            expected = "L2capFrameCodec frame length must be between 0 and 4 bytes.",
            actual = error.message,
        )
    }

    @Test
    public fun append_rejectsNegativeDecodedLengths(): Unit {
        // Arrange
        val codec = L2capFrameCodec(maxFrameLengthBytes = 4)
        val encoded = WriteBuffer().apply {
            writeInt(value = -1)
        }.toByteArray()

        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            codec.append(chunk = encoded)
        }

        // Assert
        assertEquals(
            expected = "L2capFrameCodec frame length must be between 0 and 4 bytes.",
            actual = error.message,
        )
    }

    @Test
    public fun init_rejectsNegativeMaximumFrameLengths(): Unit {
        // Arrange
        val expectedMessage = "L2capFrameCodec maxFrameLengthBytes must be greater than or equal to 0."

        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            L2capFrameCodec(maxFrameLengthBytes = -1)
        }

        // Assert
        assertEquals(expected = expectedMessage, actual = error.message)
    }
}
