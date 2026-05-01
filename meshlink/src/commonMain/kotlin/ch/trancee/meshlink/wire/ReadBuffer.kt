package ch.trancee.meshlink.wire

public class ReadBuffer(
    private val source: ByteArray,
) {
    private var position: Int = 0

    public val remaining: Int
        get() = source.size - position

    public fun readByte(): Byte {
        ensureAvailable(requiredBytes = 1)
        return source[position++]
    }

    public fun readInt(): Int {
        ensureAvailable(requiredBytes = Int.SIZE_BYTES)

        val b0: Int = source[position++].toInt() and 0xFF
        val b1: Int = source[position++].toInt() and 0xFF
        val b2: Int = source[position++].toInt() and 0xFF
        val b3: Int = source[position++].toInt() and 0xFF

        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }

    public fun readBytes(length: Int): ByteArray {
        if (length < 0) {
            throw IllegalArgumentException("ReadBuffer cannot read a negative number of bytes.")
        }
        ensureAvailable(requiredBytes = length)

        val bytes: ByteArray = source.copyOfRange(fromIndex = position, toIndex = position + length)
        position += length
        return bytes
    }

    private fun ensureAvailable(requiredBytes: Int): Unit {
        if (remaining < requiredBytes) {
            throw IllegalArgumentException(
                "ReadBuffer underflow: required $requiredBytes bytes but only $remaining remain.",
            )
        }
    }
}
