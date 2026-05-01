package ch.trancee.meshlink.crypto.noise

/** Small LRU-style cache for Diffie-Hellman results. */
public class DhCache(maxEntries: Int = DEFAULT_MAX_ENTRIES) {
  private val capacity: Int = maxEntries.coerceAtLeast(minimumValue = 1)
  private val entries: MutableList<Pair<DhCacheKey, ByteArray>> = mutableListOf()

  /** Returns a cached DH result when present, otherwise computes and stores it. */
  public fun getOrCompute(
    privateKey: ByteArray,
    publicKey: ByteArray,
    compute: () -> ByteArray,
  ): ByteArray {
    val cacheKey = DhCacheKey(privateKey = privateKey, publicKey = publicKey)
    val existingIndex: Int = entries.indexOfFirst { entry -> entry.first == cacheKey }
    if (existingIndex >= 0) {
      val cached: ByteArray = entries.removeAt(existingIndex).second
      entries.add(cacheKey to cached)
      return cached.copyOf()
    }

    val computed: ByteArray = compute()
    if (entries.size == capacity) {
      entries.removeAt(index = 0)
    }
    entries.add(cacheKey to computed.copyOf())
    return computed.copyOf()
  }

  public companion object {
    public const val DEFAULT_MAX_ENTRIES: Int = 64
  }
}
