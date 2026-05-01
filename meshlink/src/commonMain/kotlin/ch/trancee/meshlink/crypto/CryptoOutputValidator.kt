package ch.trancee.meshlink.crypto

internal object CryptoOutputValidator {
  internal fun requireExactSize(bytes: ByteArray, expectedSize: Int, label: String): ByteArray {
    require(bytes.size == expectedSize) { "$label must be exactly $expectedSize bytes." }
    return bytes.copyOf()
  }
}
