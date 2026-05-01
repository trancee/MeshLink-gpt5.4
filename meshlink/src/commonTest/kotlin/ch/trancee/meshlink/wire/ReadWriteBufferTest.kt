package ch.trancee.meshlink.wire

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class ReadWriteBufferTest {
    @Test
    public fun writeIntAndReadInt_roundTripLittleEndianValue(): Unit {
        // Arrange
        val expectedValue: Int = 0x78563412
        val writeBuffer = WriteBuffer(initialCapacity = 1)

        // Act
        writeBuffer.writeInt(expectedValue)
        val actualValue: Int = ReadBuffer(source = writeBuffer.toByteArray()).readInt()

        // Assert
        assertEquals(
            expected = expectedValue,
            actual = actualValue,
            message = "ReadBuffer and WriteBuffer should round-trip Int values in little-endian order",
        )
    }

    @Test
    public fun writeLongAndReadLong_roundTripLittleEndianValue(): Unit {
        // Arrange
        val expectedValue: Long = 0x8877665544332211uL.toLong()
        val writeBuffer = WriteBuffer(initialCapacity = 1)

        // Act
        writeBuffer.writeLong(expectedValue)
        val actualValue: Long = ReadBuffer(source = writeBuffer.toByteArray()).readLong()

        // Assert
        assertEquals(
            expected = expectedValue,
            actual = actualValue,
            message = "ReadBuffer and WriteBuffer should round-trip Long values in little-endian order",
        )
    }

    @Test
    public fun writeBytes_growsBufferWhenCapacityIsExceeded(): Unit {
        // Arrange
        val expectedBytes: ByteArray = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val writeBuffer = WriteBuffer(initialCapacity = 1)

        // Act
        writeBuffer.writeBytes(expectedBytes)
        val actualBytes: ByteArray = writeBuffer.toByteArray()

        // Assert
        assertContentEquals(
            expected = expectedBytes,
            actual = actualBytes,
            message = "WriteBuffer should grow automatically when additional capacity is required",
        )
    }

    @Test
    public fun writeByte_supportsZeroInitialCapacityByNormalizingToOneByte(): Unit {
        // Arrange
        val expectedByte: Byte = 0x5A
        val writeBuffer = WriteBuffer(initialCapacity = 0)

        // Act
        writeBuffer.writeByte(expectedByte)
        val actualBytes: ByteArray = writeBuffer.toByteArray()

        // Assert
        assertContentEquals(
            expected = byteArrayOf(expectedByte),
            actual = actualBytes,
            message = "WriteBuffer should normalize zero initial capacity and still support single-byte writes",
        )
    }

    @Test
    public fun writeByte_supportsDefaultInitialCapacity(): Unit {
        // Arrange
        val expectedByte: Byte = 0x33
        val writeBuffer = WriteBuffer()

        // Act
        writeBuffer.writeByte(expectedByte)
        val actualBytes: ByteArray = writeBuffer.toByteArray()

        // Assert
        assertContentEquals(
            expected = byteArrayOf(expectedByte),
            actual = actualBytes,
            message = "WriteBuffer should use its default initial capacity when no explicit capacity is provided",
        )
    }

    @Test
    public fun readBytes_throwsWhenNegativeLengthIsRequested(): Unit {
        // Arrange
        val readBuffer = ReadBuffer(source = byteArrayOf(0x01))

        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            readBuffer.readBytes(length = -1)
        }

        // Assert
        assertEquals(
            expected = "ReadBuffer cannot read a negative number of bytes.",
            actual = error.message,
            message = "ReadBuffer should reject negative byte counts",
        )
    }

    @Test
    public fun readBytes_throwsWhenRequestedBytesExceedRemainingContent(): Unit {
        // Arrange
        val readBuffer = ReadBuffer(source = byteArrayOf(0x01, 0x02))

        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            readBuffer.readBytes(length = 3)
        }

        // Assert
        assertEquals(
            expected = "ReadBuffer underflow: required 3 bytes but only 2 remain.",
            actual = error.message,
            message = "ReadBuffer should reject reads that exceed the remaining buffer contents",
        )
    }

    @Test
    public fun readByte_readsSingleByteAndUpdatesRemainingCount(): Unit {
        // Arrange
        val readBuffer = ReadBuffer(source = byteArrayOf(0x2A, 0x7F))

        // Act
        val actualByte: Byte = readBuffer.readByte()
        val actualRemaining: Int = readBuffer.remaining

        // Assert
        assertEquals(
            expected = 0x2A,
            actual = actualByte.toInt() and 0xFF,
            message = "ReadBuffer should return the next byte in sequence",
        )
        assertEquals(
            expected = 1,
            actual = actualRemaining,
            message = "ReadBuffer should reduce the remaining byte count after a read",
        )
    }
}
