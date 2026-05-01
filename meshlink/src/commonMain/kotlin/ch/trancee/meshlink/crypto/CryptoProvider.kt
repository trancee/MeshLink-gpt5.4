package ch.trancee.meshlink.crypto

/**
 * Platform crypto abstraction used by MeshLink.
 *
 * The interface collects every primitive the protocol stack needs so common code can remain
 * platform-neutral.
 */
public interface CryptoProvider {
  /** Generates an X25519 key pair for Diffie-Hellman exchange. */
  public fun generateX25519KeyPair(): KeyPair

  /** Generates an Ed25519 key pair for signatures and identity. */
  public fun generateEd25519KeyPair(): KeyPair

  /** Computes an X25519 shared secret. */
  public fun x25519(privateKey: ByteArray, publicKey: ByteArray): ByteArray

  /** Signs a message with Ed25519. */
  public fun ed25519Sign(privateKey: ByteArray, message: ByteArray): ByteArray

  /** Verifies an Ed25519 signature. */
  public fun ed25519Verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean

  /** Encrypts and authenticates plaintext with ChaCha20-Poly1305. */
  public fun chaCha20Poly1305Encrypt(
    key: ByteArray,
    nonce: ByteArray,
    aad: ByteArray,
    plaintext: ByteArray,
  ): ByteArray

  /** Decrypts and authenticates ciphertext with ChaCha20-Poly1305. */
  public fun chaCha20Poly1305Decrypt(
    key: ByteArray,
    nonce: ByteArray,
    aad: ByteArray,
    ciphertext: ByteArray,
  ): ByteArray

  /** Derives key material with HKDF-SHA256. */
  public fun hkdfSha256(
    ikm: ByteArray,
    salt: ByteArray,
    info: ByteArray,
    outputLength: Int,
  ): ByteArray

  /** Computes HMAC-SHA256. */
  public fun hmacSha256(key: ByteArray, message: ByteArray): ByteArray
}
