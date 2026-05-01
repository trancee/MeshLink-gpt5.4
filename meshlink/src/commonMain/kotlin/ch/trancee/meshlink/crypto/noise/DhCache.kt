package ch.trancee.meshlink.crypto.noise

public class DhCache(
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES,
) {
    private val entries: LinkedHashMap<DhCacheKey, ByteArray> = object : LinkedHashMap<DhCacheKey, ByteArray>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<DhCacheKey, ByteArray>?): Boolean {
            return size > maxEntries
        }
    }

    public fun getOrCompute(
        privateKey: ByteArray,
        publicKey: ByteArray,
        compute: () -> ByteArray,
    ): ByteArray {
        val cacheKey = DhCacheKey(privateKey = privateKey, publicKey = publicKey)
        val cached: ByteArray? = entries[cacheKey]
        if (cached != null) {
            return cached.copyOf()
        }

        val computed: ByteArray = compute()
        entries[cacheKey] = computed.copyOf()
        return computed.copyOf()
    }

    public companion object {
        public const val DEFAULT_MAX_ENTRIES: Int = 64
    }
}
