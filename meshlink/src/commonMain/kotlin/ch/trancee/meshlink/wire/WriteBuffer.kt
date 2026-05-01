package ch.trancee.meshlink.wire

public class WriteBuffer(
    initialCapacity: Int = 16,
) {
    private var buffer: ByteArray = ByteArray(size = initialCapacity.coerceAtLeast(minimumValue = 1))
    private var position: Int = 0

    public fun writeByte(value: Byte): Unit {
        ensureCapacity(additionalBytes = 1)
        buffer[position++] = value
    }

    public fun writeInt(value: Int): Unit {
        ensureCapacity(additionalBytes = Int.SIZE_BYTES)

        buffer[position++] = (value and 0xFF).toByte()
        buffer[position++] = ((value ushr 8) and 0xFF).toByte()
        buffer[position++] = ((value ushr 16) and 0xFF).toByte()
        buffer[position++] = ((value ushr 24) and 0xFF).toByte()
    }

    public fun writeLong(value: Long): Unit {
        ensureCapacity(additionalBytes = Long.SIZE_BYTES)

        buffer[position++] = (value and 0xFFL).toByte()
        buffer[position++] = ((value ushr 8) and 0xFFL).toByte()
        buffer[position++] = ((value ushr 16) and 0xFFL).toByte()
        buffer[position++] = ((value ushr 24) and 0xFFL).toByte()
        buffer[position++] = ((value ushr 32) and 0xFFL).toByte()
        buffer[position++] = ((value ushr 40) and 0xFFL).toByte()
        buffer[position++] = ((value ushr 48) and 0xFFL).toByte()
        buffer[position++] = ((value ushr 56) and 0xFFL).toByte()
    }

    public fun writeBytes(value: ByteArray): Unit {
        ensureCapacity(additionalBytes = value.size)
        value.copyInto(destination = buffer, destinationOffset = position)
        position += value.size
    }

    public fun toByteArray(): ByteArray {
        return buffer.copyOf(newSize = position)
    }

    private fun ensureCapacity(additionalBytes: Int): Unit {
        val requiredCapacity: Int = position + additionalBytes
        if (requiredCapacity <= buffer.size) {
            return
        }

        var newCapacity: Int = buffer.size
        while (newCapacity < requiredCapacity) {
            newCapacity *= 2
        }
        buffer = buffer.copyOf(newSize = newCapacity)
    }
}
