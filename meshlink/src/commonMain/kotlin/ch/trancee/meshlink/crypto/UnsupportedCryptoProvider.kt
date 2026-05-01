package ch.trancee.meshlink.crypto

public open class UnsupportedCryptoProvider : CryptoProvider {
  override fun generateX25519KeyPair(): KeyPair = unsupported()

  override fun generateEd25519KeyPair(): KeyPair = unsupported()

  override fun x25519(privateKey: ByteArray, publicKey: ByteArray): ByteArray = unsupported()

  override fun ed25519Sign(privateKey: ByteArray, message: ByteArray): ByteArray = unsupported()

  override fun ed25519Verify(
    publicKey: ByteArray,
    message: ByteArray,
    signature: ByteArray,
  ): Boolean = unsupported()

  override fun chaCha20Poly1305Encrypt(
    key: ByteArray,
    nonce: ByteArray,
    aad: ByteArray,
    plaintext: ByteArray,
  ): ByteArray = unsupported()

  override fun chaCha20Poly1305Decrypt(
    key: ByteArray,
    nonce: ByteArray,
    aad: ByteArray,
    ciphertext: ByteArray,
  ): ByteArray = unsupported()

  override fun hkdfSha256(
    ikm: ByteArray,
    salt: ByteArray,
    info: ByteArray,
    outputLength: Int,
  ): ByteArray = unsupported()

  override fun hmacSha256(key: ByteArray, message: ByteArray): ByteArray = unsupported()

  private fun <T> unsupported(): T {
    throw UnsupportedOperationException(
      "CryptoProviderFactory has not been wired to a platform crypto backend yet."
    )
  }
}
