package ch.trancee.meshlink.routing

public class DedupSet(
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES,
    private val expiryMillis: Long = DEFAULT_EXPIRY_MILLIS,
) {
    private val entries: MutableList<DedupEntry> = mutableListOf()

    init {
        require(maxEntries > 0) {
            "DedupSet maxEntries must be greater than 0."
        }
        require(expiryMillis > 0) {
            "DedupSet expiryMillis must be greater than 0."
        }
    }

    public fun isDuplicate(
        key: ByteArray,
        nowEpochMillis: Long,
    ): Boolean {
        require(key.isNotEmpty()) {
            "DedupSet key must not be empty."
        }
        require(nowEpochMillis >= 0) {
            "DedupSet nowEpochMillis must be greater than or equal to 0."
        }

        removeExpiredEntries(nowEpochMillis = nowEpochMillis)

        val normalizedKey: String = key.toHexString()
        val existingIndex: Int = entries.indexOfFirst { entry -> entry.key == normalizedKey }
        if (existingIndex >= 0) {
            val existingEntry: DedupEntry = entries.removeAt(index = existingIndex)
            entries += existingEntry.copy(lastSeenEpochMillis = nowEpochMillis)
            return true
        }

        if (entries.size == maxEntries) {
            entries.removeAt(index = 0)
        }
        entries += DedupEntry(
            key = normalizedKey,
            lastSeenEpochMillis = nowEpochMillis,
        )
        return false
    }

    public fun size(): Int {
        return entries.size
    }

    public companion object {
        public const val DEFAULT_MAX_ENTRIES: Int = 256
        public const val DEFAULT_EXPIRY_MILLIS: Long = 30_000L
    }

    private fun removeExpiredEntries(nowEpochMillis: Long): Unit {
        entries.removeAll { entry -> nowEpochMillis - entry.lastSeenEpochMillis >= expiryMillis }
    }
}

private data class DedupEntry(
    val key: String,
    val lastSeenEpochMillis: Long,
)

private fun ByteArray.toHexString(): String {
    return joinToString(separator = "") { byte ->
        (byte.toInt() and 0xFF).toString(radix = 16).padStart(length = 2, padChar = '0')
    }
}
