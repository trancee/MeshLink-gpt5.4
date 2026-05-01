package ch.trancee.meshlink.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class UnsupportedCryptoProviderTest {
  @Test
  public fun allPrimitiveOperations_throwHelpfulPlaceholderMessage(): Unit {
    // Arrange
    val provider = UnsupportedCryptoProvider()

    // Act
    val generateX25519Error =
      assertFailsWith<UnsupportedOperationException> { provider.generateX25519KeyPair() }
    val generateEd25519Error =
      assertFailsWith<UnsupportedOperationException> { provider.generateEd25519KeyPair() }
    val x25519Error =
      assertFailsWith<UnsupportedOperationException> {
        provider.x25519(privateKey = byteArrayOf(0x01), publicKey = byteArrayOf(0x02))
      }
    val signError =
      assertFailsWith<UnsupportedOperationException> {
        provider.ed25519Sign(privateKey = byteArrayOf(0x03), message = byteArrayOf(0x04))
      }
    val verifyError =
      assertFailsWith<UnsupportedOperationException> {
        provider.ed25519Verify(
          publicKey = byteArrayOf(0x05),
          message = byteArrayOf(0x06),
          signature = byteArrayOf(0x07),
        )
      }
    val encryptError =
      assertFailsWith<UnsupportedOperationException> {
        provider.chaCha20Poly1305Encrypt(
          key = byteArrayOf(0x08),
          nonce = byteArrayOf(0x09),
          aad = byteArrayOf(0x0A),
          plaintext = byteArrayOf(0x0B),
        )
      }
    val decryptError =
      assertFailsWith<UnsupportedOperationException> {
        provider.chaCha20Poly1305Decrypt(
          key = byteArrayOf(0x0C),
          nonce = byteArrayOf(0x0D),
          aad = byteArrayOf(0x0E),
          ciphertext = byteArrayOf(0x0F),
        )
      }
    val hkdfError =
      assertFailsWith<UnsupportedOperationException> {
        provider.hkdfSha256(
          ikm = byteArrayOf(0x10),
          salt = byteArrayOf(0x11),
          info = byteArrayOf(0x12),
          outputLength = 32,
        )
      }
    val hmacError =
      assertFailsWith<UnsupportedOperationException> {
        provider.hmacSha256(key = byteArrayOf(0x13), message = byteArrayOf(0x14))
      }

    // Assert
    val expectedMessage =
      "CryptoProviderFactory has not been wired to a platform crypto backend yet."
    assertEquals(expected = expectedMessage, actual = generateX25519Error.message)
    assertEquals(expected = expectedMessage, actual = generateEd25519Error.message)
    assertEquals(expected = expectedMessage, actual = x25519Error.message)
    assertEquals(expected = expectedMessage, actual = signError.message)
    assertEquals(expected = expectedMessage, actual = verifyError.message)
    assertEquals(expected = expectedMessage, actual = encryptError.message)
    assertEquals(expected = expectedMessage, actual = decryptError.message)
    assertEquals(expected = expectedMessage, actual = hkdfError.message)
    assertEquals(expected = expectedMessage, actual = hmacError.message)
  }
}
