package ch.trancee.meshlink.crypto.noise

import ch.trancee.meshlink.crypto.CryptoProvider
import ch.trancee.meshlink.crypto.KeyPair
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

public class CipherStateTest {
  @Test
  public fun initializeKey_setsKeyAndResetsNonce(): Unit {
    // Arrange
    val state = CipherState(provider = FakeCipherCryptoProvider())

    // Act
    state.initializeKey(
      key = ByteArray(size = CipherState.KEY_SIZE) { index -> (index + 1).toByte() }
    )

    // Assert
    assertTrue(
      actual = state.hasKey(),
      message = "CipherState should report that a key is present after initialization",
    )
    assertEquals(
      expected = 0u,
      actual = state.nonce(),
      message = "CipherState should reset the nonce when a new key is initialized",
    )
  }

  @Test
  public fun initializeKey_clearsKeyWhenNullIsProvided(): Unit {
    // Arrange
    val state = CipherState(provider = FakeCipherCryptoProvider())
    state.initializeKey(key = ByteArray(size = CipherState.KEY_SIZE))

    // Act
    state.initializeKey(key = null)

    // Assert
    assertFalse(
      actual = state.hasKey(),
      message = "CipherState should clear the key when initialized with null",
    )
    assertEquals(
      expected = 0u,
      actual = state.nonce(),
      message = "CipherState should reset the nonce when the key is cleared",
    )
  }

  @Test
  public fun initializeKey_throwsWhenKeyLengthIsInvalid(): Unit {
    // Arrange
    val state = CipherState(provider = FakeCipherCryptoProvider())

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> { state.initializeKey(key = byteArrayOf(0x01)) }

    // Assert
    assertEquals(
      expected = "CipherState key must be exactly 32 bytes.",
      actual = error.message,
      message = "CipherState should reject malformed cipher keys",
    )
  }

  @Test
  public fun encryptWithAd_returnsPlaintextWhenKeyIsAbsent(): Unit {
    // Arrange
    val state = CipherState(provider = FakeCipherCryptoProvider())
    val plaintext: ByteArray = byteArrayOf(0x11, 0x12)

    // Act
    val actual: ByteArray = state.encryptWithAd(aad = byteArrayOf(0x01), plaintext = plaintext)

    // Assert
    assertContentEquals(
      expected = plaintext,
      actual = actual,
      message = "CipherState should echo plaintext when no cipher key is active",
    )
    assertEquals(
      expected = 0u,
      actual = state.nonce(),
      message = "CipherState should not advance the nonce when no cipher key is active",
    )
  }

  @Test
  public fun decryptWithAd_returnsCiphertextWhenKeyIsAbsent(): Unit {
    // Arrange
    val state = CipherState(provider = FakeCipherCryptoProvider())
    val ciphertext: ByteArray = byteArrayOf(0x21, 0x22)

    // Act
    val actual: ByteArray = state.decryptWithAd(aad = byteArrayOf(0x02), ciphertext = ciphertext)

    // Assert
    assertContentEquals(
      expected = ciphertext,
      actual = actual,
      message = "CipherState should echo ciphertext when no cipher key is active",
    )
    assertEquals(
      expected = 0u,
      actual = state.nonce(),
      message = "CipherState should not advance the nonce when no cipher key is active",
    )
  }

  @Test
  public fun encryptWithAd_usesCurrentNonceAndAdvancesCounter(): Unit {
    // Arrange
    val provider = FakeCipherCryptoProvider()
    provider.nextEncryptResult = byteArrayOf(0x31, 0x32)
    val state = CipherState(provider = provider)
    val key: ByteArray = ByteArray(size = CipherState.KEY_SIZE) { index -> (index + 1).toByte() }
    state.initializeKey(key = key)

    // Act
    val actual: ByteArray =
      state.encryptWithAd(aad = byteArrayOf(0x41), plaintext = byteArrayOf(0x51))

    // Assert
    assertContentEquals(
      expected = byteArrayOf(0x31, 0x32),
      actual = actual,
      message = "CipherState should return the provider-produced ciphertext",
    )
    assertContentEquals(
      expected = key,
      actual = provider.lastEncryptKey,
      message = "CipherState should encrypt using the initialized cipher key",
    )
    assertContentEquals(
      expected =
        byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00),
      actual = provider.lastEncryptNonce,
      message = "CipherState should start encryption at nonce 0 encoded as a 96-bit nonce",
    )
    assertEquals(
      expected = 1u,
      actual = state.nonce(),
      message = "CipherState should advance the nonce after encryption",
    )
  }

  @Test
  public fun decryptWithAd_usesCurrentNonceAndAdvancesCounter(): Unit {
    // Arrange
    val provider = FakeCipherCryptoProvider()
    provider.nextDecryptResult = byteArrayOf(0x61, 0x62)
    val state = CipherState(provider = provider)
    state.initializeKey(
      key = ByteArray(size = CipherState.KEY_SIZE) { index -> (index + 1).toByte() }
    )

    // Act
    val actual: ByteArray =
      state.decryptWithAd(aad = byteArrayOf(0x71), ciphertext = byteArrayOf(0x72, 0x73))

    // Assert
    assertContentEquals(
      expected = byteArrayOf(0x61, 0x62),
      actual = actual,
      message = "CipherState should return the provider-produced plaintext",
    )
    assertContentEquals(
      expected =
        byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00),
      actual = provider.lastDecryptNonce,
      message = "CipherState should use the current nonce when decrypting",
    )
    assertEquals(
      expected = 1u,
      actual = state.nonce(),
      message = "CipherState should advance the nonce after decryption",
    )
  }

  @Test
  public fun encryptWithAd_throwsWhenNonceWouldOverflow(): Unit {
    // Arrange
    val provider = FakeCipherCryptoProvider()
    val state = CipherState(provider = provider)
    state.initializeKey(key = ByteArray(size = CipherState.KEY_SIZE))
    state.setNonceForTesting(value = ULong.MAX_VALUE)

    // Act
    val error =
      assertFailsWith<IllegalStateException> {
        state.encryptWithAd(aad = byteArrayOf(), plaintext = byteArrayOf())
      }

    // Assert
    assertEquals(
      expected = "CipherState nonce overflow requires rekey.",
      actual = error.message,
      message = "CipherState should reject encryption once the 64-bit nonce space is exhausted",
    )
  }

  private class FakeCipherCryptoProvider : CryptoProvider {
    var nextEncryptResult: ByteArray = byteArrayOf()
    var nextDecryptResult: ByteArray = byteArrayOf()
    var lastEncryptKey: ByteArray = byteArrayOf()
    var lastEncryptNonce: ByteArray = byteArrayOf()
    var lastDecryptNonce: ByteArray = byteArrayOf()

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
    ): ByteArray {
      lastEncryptKey = key.copyOf()
      lastEncryptNonce = nonce.copyOf()
      return nextEncryptResult.copyOf()
    }

    override fun chaCha20Poly1305Decrypt(
      key: ByteArray,
      nonce: ByteArray,
      aad: ByteArray,
      ciphertext: ByteArray,
    ): ByteArray {
      lastDecryptNonce = nonce.copyOf()
      return nextDecryptResult.copyOf()
    }

    override fun hkdfSha256(
      ikm: ByteArray,
      salt: ByteArray,
      info: ByteArray,
      outputLength: Int,
    ): ByteArray = unsupported()

    override fun hmacSha256(key: ByteArray, message: ByteArray): ByteArray = unsupported()

    private fun <T> unsupported(): T {
      throw UnsupportedOperationException("not used in test")
    }
  }
}
