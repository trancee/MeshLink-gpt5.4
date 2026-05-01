package ch.trancee.meshlink.crypto

public object ConstantTimeEquals {
    public fun bytes(
        left: ByteArray,
        right: ByteArray,
    ): Boolean {
        val maxLength: Int = maxOf(left.size, right.size)
        var difference: Int = left.size xor right.size

        for (index in 0 until maxLength) {
            val leftByte: Int = if (index < left.size) {
                left[index].toInt() and 0xFF
            } else {
                0
            }
            val rightByte: Int = if (index < right.size) {
                right[index].toInt() and 0xFF
            } else {
                0
            }
            difference = difference or (leftByte xor rightByte)
        }

        return difference == 0
    }
}
