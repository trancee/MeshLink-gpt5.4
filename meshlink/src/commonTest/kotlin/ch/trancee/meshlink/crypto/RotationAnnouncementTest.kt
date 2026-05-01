package ch.trancee.meshlink.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

public class RotationAnnouncementTest {
  @Test
  public fun create_signsPreviousAndNextPublicKeys(): Unit {
    // Arrange
    val previousIdentity =
      Identity(
        publicKey = ByteArray(size = Identity.PUBLIC_KEY_SIZE) { index -> (index + 1).toByte() },
        secretKey = ByteArray(size = Identity.SECRET_KEY_SIZE) { index -> (index + 33).toByte() },
        keyHash = byteArrayOf(0x01),
      )
    val nextPublicKey: ByteArray =
      ByteArray(size = Identity.PUBLIC_KEY_SIZE) { index -> (index + 65).toByte() }
    val expectedSignature: ByteArray =
      ByteArray(size = RotationAnnouncement.SIGNATURE_SIZE) { index -> (index + 97).toByte() }
    val provider =
      FakeRotationCryptoProvider(signatureToReturn = expectedSignature, verifyResult = true)

    // Act
    val actual: RotationAnnouncement =
      RotationAnnouncement.create(
        provider = provider,
        previousIdentity = previousIdentity,
        nextPublicKey = nextPublicKey,
      )

    // Assert
    assertContentEquals(
      expected = previousIdentity.publicKey,
      actual = actual.previousPublicKey,
      message = "RotationAnnouncement.create should retain the previous public key",
    )
    assertContentEquals(
      expected = nextPublicKey,
      actual = actual.nextPublicKey,
      message = "RotationAnnouncement.create should retain the next public key",
    )
    assertContentEquals(
      expected = expectedSignature,
      actual = actual.signature,
      message = "RotationAnnouncement.create should retain the provider-generated signature",
    )
    assertContentEquals(
      expected = previousIdentity.secretKey,
      actual = provider.lastSignedPrivateKey,
      message = "RotationAnnouncement.create should sign with the previous identity secret key",
    )
    assertContentEquals(
      expected = previousIdentity.publicKey + nextPublicKey,
      actual = provider.lastSignedMessage,
      message =
        "RotationAnnouncement.create should sign the concatenated previous and next public keys",
    )
  }

  @Test
  public fun create_throwsWhenNextPublicKeyLengthIsInvalid(): Unit {
    // Arrange
    val provider =
      FakeRotationCryptoProvider(
        signatureToReturn = ByteArray(size = RotationAnnouncement.SIGNATURE_SIZE),
        verifyResult = true,
      )
    val previousIdentity =
      Identity(
        publicKey = ByteArray(size = Identity.PUBLIC_KEY_SIZE),
        secretKey = ByteArray(size = Identity.SECRET_KEY_SIZE),
        keyHash = byteArrayOf(0x01),
      )

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> {
        RotationAnnouncement.create(
          provider = provider,
          previousIdentity = previousIdentity,
          nextPublicKey = byteArrayOf(0x01),
        )
      }

    // Assert
    assertEquals(
      expected = "RotationAnnouncement nextPublicKey must be exactly 32 bytes.",
      actual = error.message,
      message = "RotationAnnouncement should reject malformed next public keys",
    )
  }

  @Test
  public fun create_throwsWhenPreviousSecretKeyLengthIsInvalid(): Unit {
    // Arrange
    val provider =
      FakeRotationCryptoProvider(
        signatureToReturn = ByteArray(size = RotationAnnouncement.SIGNATURE_SIZE),
        verifyResult = true,
      )
    val previousIdentity =
      Identity(
        publicKey = ByteArray(size = Identity.PUBLIC_KEY_SIZE),
        secretKey = byteArrayOf(0x01),
        keyHash = byteArrayOf(0x01),
      )

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> {
        RotationAnnouncement.create(
          provider = provider,
          previousIdentity = previousIdentity,
          nextPublicKey = ByteArray(size = Identity.PUBLIC_KEY_SIZE),
        )
      }

    // Assert
    assertEquals(
      expected = "RotationAnnouncement previous secretKey must be exactly 64 bytes.",
      actual = error.message,
      message = "RotationAnnouncement should reject malformed previous secret keys",
    )
  }

  @Test
  public fun verify_returnsTrueWhenProviderAcceptsSignature(): Unit {
    // Arrange
    val announcement =
      RotationAnnouncement(
        previousPublicKey =
          ByteArray(size = Identity.PUBLIC_KEY_SIZE) { index -> (index + 1).toByte() },
        nextPublicKey =
          ByteArray(size = Identity.PUBLIC_KEY_SIZE) { index -> (index + 33).toByte() },
        signature =
          ByteArray(size = RotationAnnouncement.SIGNATURE_SIZE) { index -> (index + 65).toByte() },
      )
    val provider =
      FakeRotationCryptoProvider(
        signatureToReturn = ByteArray(size = RotationAnnouncement.SIGNATURE_SIZE),
        verifyResult = true,
      )

    // Act
    val actual: Boolean = announcement.verify(provider = provider)

    // Assert
    assertTrue(
      actual = actual,
      message =
        "RotationAnnouncement.verify should return true when the provider accepts the signature",
    )
    assertContentEquals(
      expected = announcement.previousPublicKey,
      actual = provider.lastVerifiedPublicKey,
      message = "RotationAnnouncement.verify should verify against the previous public key",
    )
    assertContentEquals(
      expected = announcement.previousPublicKey + announcement.nextPublicKey,
      actual = provider.lastVerifiedMessage,
      message =
        "RotationAnnouncement.verify should verify the concatenated previous and next public keys",
    )
    assertContentEquals(
      expected = announcement.signature,
      actual = provider.lastVerifiedSignature,
      message = "RotationAnnouncement.verify should pass the stored signature to the provider",
    )
  }

  @Test
  public fun verify_returnsFalseWhenProviderRejectsSignature(): Unit {
    // Arrange
    val announcement =
      RotationAnnouncement(
        previousPublicKey = ByteArray(size = Identity.PUBLIC_KEY_SIZE),
        nextPublicKey = ByteArray(size = Identity.PUBLIC_KEY_SIZE),
        signature = ByteArray(size = RotationAnnouncement.SIGNATURE_SIZE),
      )
    val provider =
      FakeRotationCryptoProvider(
        signatureToReturn = ByteArray(size = RotationAnnouncement.SIGNATURE_SIZE),
        verifyResult = false,
      )

    // Act
    val actual: Boolean = announcement.verify(provider = provider)

    // Assert
    assertFalse(
      actual = actual,
      message =
        "RotationAnnouncement.verify should return false when the provider rejects the signature",
    )
  }

  @Test
  public fun verify_throwsWhenSignatureLengthIsInvalid(): Unit {
    // Arrange
    val announcement =
      RotationAnnouncement(
        previousPublicKey = ByteArray(size = Identity.PUBLIC_KEY_SIZE),
        nextPublicKey = ByteArray(size = Identity.PUBLIC_KEY_SIZE),
        signature = byteArrayOf(0x01),
      )
    val provider =
      FakeRotationCryptoProvider(
        signatureToReturn = ByteArray(size = RotationAnnouncement.SIGNATURE_SIZE),
        verifyResult = true,
      )

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> { announcement.verify(provider = provider) }

    // Assert
    assertEquals(
      expected = "RotationAnnouncement signature must be exactly 64 bytes.",
      actual = error.message,
      message = "RotationAnnouncement should reject malformed signatures before verification",
    )
  }

  private class FakeRotationCryptoProvider(
    private val signatureToReturn: ByteArray,
    private val verifyResult: Boolean,
  ) : CryptoProvider {
    var lastSignedPrivateKey: ByteArray = byteArrayOf()
      private set

    var lastSignedMessage: ByteArray = byteArrayOf()
      private set

    var lastVerifiedPublicKey: ByteArray = byteArrayOf()
      private set

    var lastVerifiedMessage: ByteArray = byteArrayOf()
      private set

    var lastVerifiedSignature: ByteArray = byteArrayOf()
      private set

    override fun generateX25519KeyPair(): KeyPair = unsupported()

    override fun generateEd25519KeyPair(): KeyPair = unsupported()

    override fun x25519(privateKey: ByteArray, publicKey: ByteArray): ByteArray = unsupported()

    override fun ed25519Sign(privateKey: ByteArray, message: ByteArray): ByteArray {
      lastSignedPrivateKey = privateKey.copyOf()
      lastSignedMessage = message.copyOf()
      return signatureToReturn.copyOf()
    }

    override fun ed25519Verify(
      publicKey: ByteArray,
      message: ByteArray,
      signature: ByteArray,
    ): Boolean {
      lastVerifiedPublicKey = publicKey.copyOf()
      lastVerifiedMessage = message.copyOf()
      lastVerifiedSignature = signature.copyOf()
      return verifyResult
    }

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
      throw UnsupportedOperationException("not used in test")
    }
  }
}
