package ch.trancee.meshlink.crypto

public interface CryptoProvider {
  public fun generateX25519KeyPair(): KeyPair

  public fun generateEd25519KeyPair(): KeyPair

  public fun x25519(privateKey: ByteArray, publicKey: ByteArray): ByteArray

  public fun ed25519Sign(privateKey: ByteArray, message: ByteArray): ByteArray

  public fun ed25519Verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean

  public fun chaCha20Poly1305Encrypt(
    key: ByteArray,
    nonce: ByteArray,
    aad: ByteArray,
    plaintext: ByteArray,
  ): ByteArray

  public fun chaCha20Poly1305Decrypt(
    key: ByteArray,
    nonce: ByteArray,
    aad: ByteArray,
    ciphertext: ByteArray,
  ): ByteArray

  public fun hkdfSha256(
    ikm: ByteArray,
    salt: ByteArray,
    info: ByteArray,
    outputLength: Int,
  ): ByteArray

  public fun hmacSha256(key: ByteArray, message: ByteArray): ByteArray
}
