package ch.trancee.meshlink.crypto.noise

import ch.trancee.meshlink.crypto.CryptoProvider

/**
 * Noise handshake symmetric state.
 *
 * It tracks the chaining key, handshake hash, and optional cipher key while the handshake is still
 * in progress.
 */
public class SymmetricState(
  private val provider: CryptoProvider,
  initialChainingKey: ByteArray,
  initialHandshakeHash: ByteArray,
) {
  private var chainingKey: ByteArray = initialChainingKey.copyOf()
  private var handshakeHash: ByteArray = initialHandshakeHash.copyOf()
  private var cipherKey: ByteArray? = null

  public fun chainingKey(): ByteArray = chainingKey.copyOf()

  public fun handshakeHash(): ByteArray = handshakeHash.copyOf()

  public fun hasCipherKey(): Boolean = cipherKey != null

  /** Mixes data into the running handshake hash. */
  public fun mixHash(data: ByteArray): ByteArray {
    handshakeHash = provider.hmacSha256(key = handshakeHash, message = data)
    return handshakeHash()
  }

  /** Mixes input key material into the chaining key and derives a fresh cipher key. */
  public fun mixKey(inputKeyMaterial: ByteArray): ByteArray {
    val derivedKeyMaterial: ByteArray =
      provider.hkdfSha256(
        ikm = inputKeyMaterial,
        salt = chainingKey,
        info = KEY_DERIVATION_INFO,
        outputLength = DERIVED_KEY_MATERIAL_SIZE,
      )
    chainingKey = derivedKeyMaterial.copyOfRange(fromIndex = 0, toIndex = KEY_SIZE)
    val newCipherKey: ByteArray =
      derivedKeyMaterial.copyOfRange(fromIndex = KEY_SIZE, toIndex = DERIVED_KEY_MATERIAL_SIZE)
    cipherKey = newCipherKey
    return newCipherKey.copyOf()
  }

  /** Encrypts handshake payload bytes, then folds the ciphertext into the handshake hash. */
  public fun encryptAndHash(plaintext: ByteArray): ByteArray {
    val ciphertext: ByteArray =
      if (cipherKey == null) {
        plaintext.copyOf()
      } else {
        provider.chaCha20Poly1305Encrypt(
          key = cipherKey!!,
          nonce = ZERO_NONCE,
          aad = handshakeHash,
          plaintext = plaintext,
        )
      }
    mixHash(data = ciphertext)
    return ciphertext
  }

  /** Decrypts handshake payload bytes, then folds the ciphertext into the handshake hash. */
  public fun decryptAndHash(ciphertext: ByteArray): ByteArray {
    val plaintext: ByteArray =
      if (cipherKey == null) {
        ciphertext.copyOf()
      } else {
        provider.chaCha20Poly1305Decrypt(
          key = cipherKey!!,
          nonce = ZERO_NONCE,
          aad = handshakeHash,
          ciphertext = ciphertext,
        )
      }
    mixHash(data = ciphertext)
    return plaintext
  }

  public companion object {
    private const val KEY_SIZE: Int = 32
    private const val DERIVED_KEY_MATERIAL_SIZE: Int = KEY_SIZE * 2
    private val KEY_DERIVATION_INFO: ByteArray =
      "meshlink-noise-symmetric-state".encodeToByteArray()
    private val ZERO_NONCE: ByteArray = ByteArray(size = 12)
  }
}
