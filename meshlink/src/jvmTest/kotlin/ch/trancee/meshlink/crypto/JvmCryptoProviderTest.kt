package ch.trancee.meshlink.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

public class JvmCryptoProviderTest {
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
        val signature: ByteArray = provider.ed25519Sign(
            privateKey = keyPair.secretKey,
            message = message,
        )
        val actual: Boolean = provider.ed25519Verify(
            publicKey = keyPair.publicKey,
            message = message,
            signature = signature,
        )

        // Assert
        assertTrue(
            actual = actual,
            message = "JvmCryptoProvider should verify signatures produced by its Ed25519 signing implementation",
        )
    }

    @Test
    public fun ed25519Verify_returnsFalseForTamperedMessage(): Unit {
        // Arrange
        val provider = JvmCryptoProvider()
        val keyPair: KeyPair = provider.generateEd25519KeyPair()
        val signature: ByteArray = provider.ed25519Sign(
            privateKey = keyPair.secretKey,
            message = "meshlink".encodeToByteArray(),
        )

        // Act
        val actual: Boolean = provider.ed25519Verify(
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
        val error = assertFailsWith<IllegalArgumentException> {
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
        val error = assertFailsWith<IllegalArgumentException> {
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
        val expected: ByteArray = hex(
            "3cb25f25faacd57a90434f64d0362f2a" +
                "2d2d0a90cf1a5a4c5db02d56ecc4c5bf" +
                "34007208d5b887185865",
        )

        // Act
        val actual: ByteArray = provider.hkdfSha256(
            ikm = ikm,
            salt = salt,
            info = info,
            outputLength = 42,
        )

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
        val actual: ByteArray = provider.hkdfSha256(
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
        val error = assertFailsWith<IllegalArgumentException> {
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
    public fun unsupportedPrimitives_throwHelpfulMessage(): Unit {
        // Arrange
        val provider = JvmCryptoProvider()

        // Act
        val generateX25519Error = assertFailsWith<UnsupportedOperationException> {
            provider.generateX25519KeyPair()
        }
        val x25519Error = assertFailsWith<UnsupportedOperationException> {
            provider.x25519(privateKey = byteArrayOf(0x01), publicKey = byteArrayOf(0x02))
        }
        val encryptError = assertFailsWith<UnsupportedOperationException> {
            provider.chaCha20Poly1305Encrypt(
                key = byteArrayOf(0x08),
                nonce = byteArrayOf(0x09),
                aad = byteArrayOf(0x0A),
                plaintext = byteArrayOf(0x0B),
            )
        }
        val decryptError = assertFailsWith<UnsupportedOperationException> {
            provider.chaCha20Poly1305Decrypt(
                key = byteArrayOf(0x0C),
                nonce = byteArrayOf(0x0D),
                aad = byteArrayOf(0x0E),
                ciphertext = byteArrayOf(0x0F),
            )
        }

        // Assert
        val expectedMessage = "JvmCryptoProvider primitive is not implemented yet."
        assertEquals(expected = expectedMessage, actual = generateX25519Error.message)
        assertEquals(expected = expectedMessage, actual = x25519Error.message)
        assertEquals(expected = expectedMessage, actual = encryptError.message)
        assertEquals(expected = expectedMessage, actual = decryptError.message)
    }

    private fun hex(value: String): ByteArray {
        return value.chunked(size = 2)
            .map { chunk -> chunk.toInt(radix = 16).toByte() }
            .toByteArray()
    }
}
