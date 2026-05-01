package ch.trancee.meshlink.crypto.noise

internal data class DhCacheKey(
  private val privateKey: ByteArray,
  private val publicKey: ByteArray,
) {
  override fun equals(other: Any?): Boolean {
    return other is DhCacheKey &&
      privateKey.contentEquals(other.privateKey) &&
      publicKey.contentEquals(other.publicKey)
  }

  override fun hashCode(): Int {
    return (31 * privateKey.contentHashCode()) + publicKey.contentHashCode()
  }
}
