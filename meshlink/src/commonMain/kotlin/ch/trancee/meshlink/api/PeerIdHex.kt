package ch.trancee.meshlink.api

public data class PeerIdHex(
    public val value: String,
) {
    init {
        require(value.isNotEmpty()) { "PeerIdHex must not be empty." }
        require(value.length % 2 == 0) { "PeerIdHex must contain an even number of hex characters." }
        require(value.all { character -> character.isDigit() || character.lowercaseChar() in 'a'..'f' }) {
            "PeerIdHex must contain only hexadecimal characters."
        }
    }

    public fun toByteArray(): ByteArray {
        return value.chunked(size = 2)
            .map { chunk -> chunk.toInt(radix = 16).toByte() }
            .toByteArray()
    }

    public companion object {
        public fun fromBytes(bytes: ByteArray): PeerIdHex {
            return PeerIdHex(
                value = bytes.joinToString(separator = "") { byte ->
                    (byte.toInt() and 0xFF).toString(radix = 16).padStart(length = 2, padChar = '0')
                },
            )
        }
    }
}
