package ch.trancee.meshlink.crypto

/** Small validation helpers for crypto output shapes. */
internal object CryptoOutputValidator {
  /** Verifies that [bytes] has the expected size and returns a defensive copy. */
  internal fun requireExactSize(bytes: ByteArray, expectedSize: Int, label: String): ByteArray {
    require(bytes.size == expectedSize) { "$label must be exactly $expectedSize bytes." }
    return bytes.copyOf()
  }
}
