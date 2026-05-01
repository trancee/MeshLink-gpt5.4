package ch.trancee.meshlink.crypto.noise

import ch.trancee.meshlink.crypto.CryptoProvider

public class CipherState(private val provider: CryptoProvider, initialNonce: ULong = 0u) {
  private var key: ByteArray? = null
  private var nonce: ULong = initialNonce

  public fun hasKey(): Boolean = key != null

  public fun nonce(): ULong = nonce

  public fun initializeKey(key: ByteArray?): Unit {
    if (key != null && key.size != KEY_SIZE) {
      throw IllegalArgumentException("CipherState key must be exactly $KEY_SIZE bytes.")
    }
    this.key = key?.copyOf()
    nonce = 0u
  }

  public fun encryptWithAd(aad: ByteArray, plaintext: ByteArray): ByteArray {
    val currentKey: ByteArray = key ?: return plaintext.copyOf()
    ensureNonceAvailable()
    val ciphertext: ByteArray =
      provider.chaCha20Poly1305Encrypt(
        key = currentKey,
        nonce = nonceBytes(value = nonce),
        aad = aad,
        plaintext = plaintext,
      )
    nonce += 1u
    return ciphertext
  }

  public fun decryptWithAd(aad: ByteArray, ciphertext: ByteArray): ByteArray {
    val currentKey: ByteArray = key ?: return ciphertext.copyOf()
    ensureNonceAvailable()
    val plaintext: ByteArray =
      provider.chaCha20Poly1305Decrypt(
        key = currentKey,
        nonce = nonceBytes(value = nonce),
        aad = aad,
        ciphertext = ciphertext,
      )
    nonce += 1u
    return plaintext
  }

  internal fun setNonceForTesting(value: ULong): Unit {
    nonce = value
  }

  private fun ensureNonceAvailable(): Unit {
    if (nonce == ULong.MAX_VALUE) {
      throw IllegalStateException("CipherState nonce overflow requires rekey.")
    }
  }

  private fun nonceBytes(value: ULong): ByteArray {
    return byteArrayOf(
      0x00,
      0x00,
      0x00,
      0x00,
      (value and 0xFFu).toByte(),
      ((value shr 8) and 0xFFu).toByte(),
      ((value shr 16) and 0xFFu).toByte(),
      ((value shr 24) and 0xFFu).toByte(),
      ((value shr 32) and 0xFFu).toByte(),
      ((value shr 40) and 0xFFu).toByte(),
      ((value shr 48) and 0xFFu).toByte(),
      ((value shr 56) and 0xFFu).toByte(),
    )
  }

  public companion object {
    public const val KEY_SIZE: Int = 32
  }
}
