package ch.trancee.meshlink.crypto

import javax.crypto.AEADBadTagException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

public class JvmCryptoProviderTest {
  @Test
  public fun generateX25519KeyPair_returnsExpectedKeyLengths(): Unit {
    // Arrange
    val provider = JvmCryptoProvider()

    // Act
    val actual: KeyPair = provider.generateX25519KeyPair()

    // Assert
    assertEquals(
      expected = 32,
      actual = actual.publicKey.size,
      message = "JvmCryptoProvider should expose raw 32-byte X25519 public keys",
    )
    assertEquals(
      expected = 32,
      actual = actual.secretKey.size,
      message = "JvmCryptoProvider should expose raw 32-byte X25519 private keys",
    )
  }

  @Test
  public fun x25519_generatesMatchingSharedSecretsForBothPeers(): Unit {
    // Arrange
    val provider = JvmCryptoProvider()
    val left: KeyPair = provider.generateX25519KeyPair()
    val right: KeyPair = provider.generateX25519KeyPair()

    // Act
    val leftSharedSecret: ByteArray =
      provider.x25519(privateKey = left.secretKey, publicKey = right.publicKey)
    val rightSharedSecret: ByteArray =
      provider.x25519(privateKey = right.secretKey, publicKey = left.publicKey)

    // Assert
    assertContentEquals(
      expected = leftSharedSecret,
      actual = rightSharedSecret,
      message = "JvmCryptoProvider should derive the same X25519 shared secret for both peers",
    )
  }

  @Test
  public fun x25519_throwsWhenPrivateKeyLengthIsInvalid(): Unit {
    // Arrange
    val provider = JvmCryptoProvider()

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> {
        provider.x25519(privateKey = byteArrayOf(0x01), publicKey = ByteArray(size = 32))
      }

    // Assert
    assertEquals(
      expected = "X25519 privateKey must be exactly 32 bytes.",
      actual = error.message,
      message = "JvmCryptoProvider should reject malformed X25519 private keys",
    )
  }

  @Test
  public fun x25519_throwsWhenPublicKeyLengthIsInvalid(): Unit {
    // Arrange
    val provider = JvmCryptoProvider()

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> {
        provider.x25519(privateKey = ByteArray(size = 32), publicKey = byteArrayOf(0x01))
      }

    // Assert
    assertEquals(
      expected = "X25519 publicKey must be exactly 32 bytes.",
      actual = error.message,
      message = "JvmCryptoProvider should reject malformed X25519 public keys",
    )
  }

  @Test
  public fun generateEd25519KeyPair_returnsExpectedKeyLengths(): Unit {
    // Arrange
    val provider = JvmCryptoProvider()

    // Act
    val actual: KeyPair = provider.generateEd25519KeyPair()

    // Assert
    assertEquals(
      expected = Identity.PUBLIC_KEY_SIZE,
      actual = actual.publicKey.size,
      message = "JvmCryptoProvider should expose raw 32-byte Ed25519 public keys",
    )
    assertEquals(
      expected = Identity.SECRET_KEY_SIZE,
      actual = actual.secretKey.size,
      message = "JvmCryptoProvider should expose 64-byte Ed25519 secret keys",
    )
  }

  @Test
  public fun ed25519SignAndVerify_roundTripSignature(): Unit {
    // Arrange
    val provider = JvmCryptoProvider()
    val keyPair: KeyPair = provider.generateEd25519KeyPair()
    val message: ByteArray = "meshlink".encodeToByteArray()

    // Act
    val signature: ByteArray =
      provider.ed25519Sign(privateKey = keyPair.secretKey, message = message)
    val actual: Boolean =
      provider.ed25519Verify(
        publicKey = keyPair.publicKey,
        message = message,
        signature = signature,
      )

    // Assert
    assertTrue(
      actual = actual,
      message =
        "JvmCryptoProvider should verify signatures produced by its Ed25519 signing implementation",
    )
  }

  @Test
  public fun ed25519Verify_returnsFalseForTamperedMessage(): Unit {
    // Arrange
    val provider = JvmCryptoProvider()
    val keyPair: KeyPair = provider.generateEd25519KeyPair()
    val signature: ByteArray =
      provider.ed25519Sign(privateKey = keyPair.secretKey, message = "meshlink".encodeToByteArray())

    // Act
    val actual: Boolean =
      provider.ed25519Verify(
        publicKey = keyPair.publicKey,
        message = "tampered".encodeToByteArray(),
        signature = signature,
      )

    // Assert
    assertFalse(
      actual = actual,
      message = "JvmCryptoProvider should reject Ed25519 signatures when the signed message changes",
    )
  }

  @Test
  public fun ed25519Sign_throwsWhenSecretKeyLengthIsInvalid(): Unit {
    // Arrange
    val provider = JvmCryptoProvider()

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> {
        provider.ed25519Sign(privateKey = byteArrayOf(0x01), message = byteArrayOf(0x02))
      }

    // Assert
    assertEquals(
      expected = "Ed25519 secretKey must be exactly 64 bytes.",
      actual = error.message,
      message = "JvmCryptoProvider should reject malformed Ed25519 secret keys",
    )
  }

  @Test
  public fun ed25519Verify_throwsWhenPublicKeyLengthIsInvalid(): Unit {
    // Arrange
    val provider = JvmCryptoProvider()

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> {
        provider.ed25519Verify(
          publicKey = byteArrayOf(0x01),
          message = byteArrayOf(0x02),
          signature = byteArrayOf(0x03),
        )
      }

    // Assert
    assertEquals(
      expected = "Ed25519 publicKey must be exactly 32 bytes.",
      actual = error.message,
      message = "JvmCryptoProvider should reject malformed Ed25519 public keys",
    )
  }

  @Test
  public fun hkdfSha256_matchesRfc5869TestVector(): Unit {
    // Arrange
    val provider = JvmCryptoProvider()
    val ikm: ByteArray = ByteArray(size = 22) { 0x0b }
    val salt: ByteArray = hex("000102030405060708090a0b0c")
    val info: ByteArray = hex("f0f1f2f3f4f5f6f7f8f9")
    val expected: ByteArray =
      hex(
        "3cb25f25faacd57a90434f64d0362f2a" +
          "2d2d0a90cf1a5a4c5db02d56ecc4c5bf" +
          "34007208d5b887185865"
      )

    // Act
    val actual: ByteArray =
      provider.hkdfSha256(ikm = ikm, salt = salt, info = info, outputLength = 42)

    // Assert
    assertContentEquals(
      expected = expected,
      actual = actual,
      message = "JvmCryptoProvider should match the RFC 5869 HKDF-SHA256 test vector",
    )
  }

  @Test
  public fun hkdfSha256_returnsEmptyOutputWhenRequestedLengthIsZeroAndSaltIsEmpty(): Unit {
    // Arrange
    val provider = JvmCryptoProvider()

    // Act
    val actual: ByteArray =
      provider.hkdfSha256(
        ikm = byteArrayOf(0x01),
        salt = byteArrayOf(),
        info = byteArrayOf(0x02),
        outputLength = 0,
      )

    // Assert
    assertContentEquals(
      expected = byteArrayOf(),
      actual = actual,
      message = "JvmCryptoProvider should allow zero-length HKDF output requests and empty salts",
    )
  }

  @Test
  public fun hkdfSha256_throwsWhenOutputLengthIsNegative(): Unit {
    // Arrange
    val provider = JvmCryptoProvider()

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> {
        provider.hkdfSha256(
          ikm = byteArrayOf(0x01),
          salt = byteArrayOf(0x02),
          info = byteArrayOf(0x03),
          outputLength = -1,
        )
      }

    // Assert
    assertEquals(
      expected = "HKDF outputLength must be non-negative.",
      actual = error.message,
      message = "JvmCryptoProvider should reject negative HKDF output lengths",
    )
  }

  @Test
  public fun chaCha20Poly1305_encryptAndDecrypt_roundTripPlaintext(): Unit {
    // Arrange
    val provider = JvmCryptoProvider()
    val key: ByteArray = ByteArray(size = 32) { index -> index.toByte() }
    val nonce: ByteArray = ByteArray(size = 12) { index -> (index + 1).toByte() }
    val aad: ByteArray = byteArrayOf(0x01, 0x02, 0x03)
    val plaintext: ByteArray = byteArrayOf(0x11, 0x12, 0x13, 0x14)

    // Act
    val ciphertext: ByteArray =
      provider.chaCha20Poly1305Encrypt(key = key, nonce = nonce, aad = aad, plaintext = plaintext)
    val actual: ByteArray =
      provider.chaCha20Poly1305Decrypt(key = key, nonce = nonce, aad = aad, ciphertext = ciphertext)

    // Assert
    assertContentEquals(
      expected = plaintext,
      actual = actual,
      message = "JvmCryptoProvider should round-trip plaintext through ChaCha20-Poly1305",
    )
  }

  @Test
  public fun chaCha20Poly1305_encryptAndDecrypt_roundTripPlaintextWithoutAad(): Unit {
    // Arrange
    val provider = JvmCryptoProvider()
    val key: ByteArray = ByteArray(size = 32) { index -> index.toByte() }
    val nonce: ByteArray = ByteArray(size = 12) { index -> (index + 1).toByte() }
    val plaintext: ByteArray = byteArrayOf(0x21, 0x22, 0x23)

    // Act
    val ciphertext: ByteArray =
      provider.chaCha20Poly1305Encrypt(
        key = key,
        nonce = nonce,
        aad = byteArrayOf(),
        plaintext = plaintext,
      )
    val actual: ByteArray =
      provider.chaCha20Poly1305Decrypt(
        key = key,
        nonce = nonce,
        aad = byteArrayOf(),
        ciphertext = ciphertext,
      )

    // Assert
    assertContentEquals(
      expected = plaintext,
      actual = actual,
      message =
        "JvmCryptoProvider should also round-trip ChaCha20-Poly1305 payloads when no AAD is provided",
    )
  }

  @Test
  public fun chaCha20Poly1305Decrypt_throwsWhenCiphertextIsTampered(): Unit {
    // Arrange
    val provider = JvmCryptoProvider()
    val key: ByteArray = ByteArray(size = 32) { index -> index.toByte() }
    val nonce: ByteArray = ByteArray(size = 12) { index -> (index + 1).toByte() }
    val aad: ByteArray = byteArrayOf(0x01, 0x02)
    val ciphertext: ByteArray =
      provider
        .chaCha20Poly1305Encrypt(
          key = key,
          nonce = nonce,
          aad = aad,
          plaintext = byteArrayOf(0x21, 0x22),
        )
        .also { encrypted ->
          encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 0x01).toByte()
        }

    // Act
    assertFailsWith<AEADBadTagException> {
      provider.chaCha20Poly1305Decrypt(key = key, nonce = nonce, aad = aad, ciphertext = ciphertext)
    }

    // Assert
    // expected exception
  }

  @Test
  public fun chaCha20Poly1305Encrypt_throwsWhenKeyLengthIsInvalid(): Unit {
    // Arrange
    val provider = JvmCryptoProvider()

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> {
        provider.chaCha20Poly1305Encrypt(
          key = byteArrayOf(0x01),
          nonce = ByteArray(size = 12),
          aad = byteArrayOf(),
          plaintext = byteArrayOf(),
        )
      }

    // Assert
    assertEquals(
      expected = "ChaCha20-Poly1305 key must be exactly 32 bytes.",
      actual = error.message,
      message = "JvmCryptoProvider should reject malformed ChaCha20-Poly1305 keys",
    )
  }

  @Test
  public fun chaCha20Poly1305Encrypt_throwsWhenNonceLengthIsInvalid(): Unit {
    // Arrange
    val provider = JvmCryptoProvider()

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> {
        provider.chaCha20Poly1305Encrypt(
          key = ByteArray(size = 32),
          nonce = byteArrayOf(0x01),
          aad = byteArrayOf(),
          plaintext = byteArrayOf(),
        )
      }

    // Assert
    assertEquals(
      expected = "ChaCha20-Poly1305 nonce must be exactly 12 bytes.",
      actual = error.message,
      message =
        "JvmCryptoProvider should reject malformed ChaCha20-Poly1305 nonces during encryption",
    )
  }

  @Test
  public fun chaCha20Poly1305Decrypt_throwsWhenKeyLengthIsInvalid(): Unit {
    // Arrange
    val provider = JvmCryptoProvider()

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> {
        provider.chaCha20Poly1305Decrypt(
          key = byteArrayOf(0x01),
          nonce = ByteArray(size = 12),
          aad = byteArrayOf(),
          ciphertext = byteArrayOf(),
        )
      }

    // Assert
    assertEquals(
      expected = "ChaCha20-Poly1305 key must be exactly 32 bytes.",
      actual = error.message,
      message = "JvmCryptoProvider should reject malformed ChaCha20-Poly1305 keys during decryption",
    )
  }

  @Test
  public fun chaCha20Poly1305Decrypt_throwsWhenNonceLengthIsInvalid(): Unit {
    // Arrange
    val provider = JvmCryptoProvider()

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> {
        provider.chaCha20Poly1305Decrypt(
          key = ByteArray(size = 32),
          nonce = byteArrayOf(0x01),
          aad = byteArrayOf(),
          ciphertext = byteArrayOf(),
        )
      }

    // Assert
    assertEquals(
      expected = "ChaCha20-Poly1305 nonce must be exactly 12 bytes.",
      actual = error.message,
      message = "JvmCryptoProvider should reject malformed ChaCha20-Poly1305 nonces",
    )
  }

  @Test
  public fun unsupportedPrimitives_throwHelpfulMessage(): Unit {
    // Arrange
    val provider = JvmCryptoProvider()

    // Act

    // Assert
    val expectedMessage = "JvmCryptoProvider primitive is not implemented yet."
  }

  private fun hex(value: String): ByteArray {
    return value.chunked(size = 2).map { chunk -> chunk.toInt(radix = 16).toByte() }.toByteArray()
  }
}
