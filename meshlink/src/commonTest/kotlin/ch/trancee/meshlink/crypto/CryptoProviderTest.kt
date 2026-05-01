package ch.trancee.meshlink.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

public class CryptoProviderTest {
  @Test
  public fun primitives_areExposedThroughTheCryptoProviderContract(): Unit {
    // Arrange
    val provider = FakeCryptoProvider()
    val expectedX25519: ByteArray = byteArrayOf(0x11, 0x12)
    val expectedSignature: ByteArray = byteArrayOf(0x21, 0x22)
    val expectedCiphertext: ByteArray = byteArrayOf(0x31, 0x32)
    val expectedPlaintext: ByteArray = byteArrayOf(0x41, 0x42)
    val expectedHkdf: ByteArray = byteArrayOf(0x51, 0x52)
    val expectedHmac: ByteArray = byteArrayOf(0x61, 0x62)
    provider.nextX25519Result = expectedX25519
    provider.nextSignature = expectedSignature
    provider.nextVerifyResult = true
    provider.nextCiphertext = expectedCiphertext
    provider.nextPlaintext = expectedPlaintext
    provider.nextHkdf = expectedHkdf
    provider.nextHmac = expectedHmac

    // Act
    val actualX25519KeyPair: KeyPair = provider.generateX25519KeyPair()
    val actualEd25519KeyPair: KeyPair = provider.generateEd25519KeyPair()
    val actualX25519: ByteArray =
      provider.x25519(privateKey = byteArrayOf(0x01), publicKey = byteArrayOf(0x02))
    val actualSignature: ByteArray =
      provider.ed25519Sign(privateKey = byteArrayOf(0x03), message = byteArrayOf(0x04))
    val actualVerify: Boolean =
      provider.ed25519Verify(
        publicKey = byteArrayOf(0x05),
        message = byteArrayOf(0x06),
        signature = byteArrayOf(0x07),
      )
    val actualCiphertext: ByteArray =
      provider.chaCha20Poly1305Encrypt(
        key = byteArrayOf(0x08),
        nonce = byteArrayOf(0x09),
        aad = byteArrayOf(0x0A),
        plaintext = byteArrayOf(0x0B),
      )
    val actualPlaintext: ByteArray =
      provider.chaCha20Poly1305Decrypt(
        key = byteArrayOf(0x0C),
        nonce = byteArrayOf(0x0D),
        aad = byteArrayOf(0x0E),
        ciphertext = byteArrayOf(0x0F),
      )
    val actualHkdf: ByteArray =
      provider.hkdfSha256(
        ikm = byteArrayOf(0x10),
        salt = byteArrayOf(0x11),
        info = byteArrayOf(0x12),
        outputLength = 2,
      )
    val actualHmac: ByteArray =
      provider.hmacSha256(key = byteArrayOf(0x13), message = byteArrayOf(0x14))

    // Assert
    assertContentEquals(
      expected = byteArrayOf(0x01),
      actual = actualX25519KeyPair.publicKey,
      message = "CryptoProvider should expose X25519 key generation through the contract",
    )
    assertContentEquals(
      expected = byteArrayOf(0x02),
      actual = actualEd25519KeyPair.publicKey,
      message = "CryptoProvider should expose Ed25519 key generation through the contract",
    )
    assertContentEquals(expected = expectedX25519, actual = actualX25519)
    assertContentEquals(expected = expectedSignature, actual = actualSignature)
    assertTrue(
      actual = actualVerify,
      message = "CryptoProvider should expose Ed25519 verification through the contract",
    )
    assertContentEquals(expected = expectedCiphertext, actual = actualCiphertext)
    assertContentEquals(expected = expectedPlaintext, actual = actualPlaintext)
    assertContentEquals(expected = expectedHkdf, actual = actualHkdf)
    assertContentEquals(expected = expectedHmac, actual = actualHmac)
    assertFalse(
      actual =
        provider
          .ed25519Verify(
            publicKey = byteArrayOf(0x05),
            message = byteArrayOf(0x06),
            signature = byteArrayOf(0x07),
          )
          .not(),
      message = "CryptoProvider fake provider should allow repeated verification calls",
    )
  }

  private class FakeCryptoProvider : CryptoProvider {
    var nextX25519Result: ByteArray = byteArrayOf()
    var nextSignature: ByteArray = byteArrayOf()
    var nextVerifyResult: Boolean = false
    var nextCiphertext: ByteArray = byteArrayOf()
    var nextPlaintext: ByteArray = byteArrayOf()
    var nextHkdf: ByteArray = byteArrayOf()
    var nextHmac: ByteArray = byteArrayOf()

    override fun generateX25519KeyPair(): KeyPair =
      KeyPair(publicKey = byteArrayOf(0x01), secretKey = byteArrayOf(0x11))

    override fun generateEd25519KeyPair(): KeyPair =
      KeyPair(publicKey = byteArrayOf(0x02), secretKey = byteArrayOf(0x12))

    override fun x25519(privateKey: ByteArray, publicKey: ByteArray): ByteArray = nextX25519Result

    override fun ed25519Sign(privateKey: ByteArray, message: ByteArray): ByteArray = nextSignature

    override fun ed25519Verify(
      publicKey: ByteArray,
      message: ByteArray,
      signature: ByteArray,
    ): Boolean = nextVerifyResult

    override fun chaCha20Poly1305Encrypt(
      key: ByteArray,
      nonce: ByteArray,
      aad: ByteArray,
      plaintext: ByteArray,
    ): ByteArray = nextCiphertext

    override fun chaCha20Poly1305Decrypt(
      key: ByteArray,
      nonce: ByteArray,
      aad: ByteArray,
      ciphertext: ByteArray,
    ): ByteArray = nextPlaintext

    override fun hkdfSha256(
      ikm: ByteArray,
      salt: ByteArray,
      info: ByteArray,
      outputLength: Int,
    ): ByteArray = nextHkdf

    override fun hmacSha256(key: ByteArray, message: ByteArray): ByteArray = nextHmac
  }
}
