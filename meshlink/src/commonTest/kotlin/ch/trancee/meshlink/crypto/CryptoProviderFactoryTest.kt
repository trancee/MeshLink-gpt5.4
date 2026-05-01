package ch.trancee.meshlink.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

public class CryptoProviderFactoryTest {
    @Test
    public fun create_returnsCryptoProviderImplementation(): Unit {
        // Arrange
        
        // Act
        val actual: CryptoProvider = CryptoProviderFactory.create()

        // Assert
        assertIs<CryptoProvider>(
            value = actual,
            message = "CryptoProviderFactory should return a CryptoProvider implementation for the current platform",
        )
    }

    @Test
    public fun generateX25519KeyPair_returnsPlatformKeyPairOrThrowsHelpfulPlaceholderError(): Unit {
        // Arrange
        val provider: CryptoProvider = CryptoProviderFactory.create()

        // Act
        val result: Result<KeyPair> = runCatching { provider.generateX25519KeyPair() }

        // Assert
        if (result.isSuccess) {
            val keyPair: KeyPair = result.getOrThrow()
            assertEquals(expected = 32, actual = keyPair.publicKey.size)
            assertEquals(expected = 32, actual = keyPair.secretKey.size)
        } else {
            assertEquals(
                expected = "CryptoProviderFactory has not been wired to a platform crypto backend yet.",
                actual = result.exceptionOrNull()?.message,
                message = "Placeholder platforms should surface the missing X25519 backend explicitly",
            )
        }
    }

    @Test
    public fun generateEd25519KeyPair_returnsPlatformKeyPairOrThrowsHelpfulPlaceholderError(): Unit {
        // Arrange
        val provider: CryptoProvider = CryptoProviderFactory.create()

        // Act
        val result: Result<KeyPair> = runCatching { provider.generateEd25519KeyPair() }

        // Assert
        if (result.isSuccess) {
            val keyPair: KeyPair = result.getOrThrow()
            assertEquals(expected = Identity.PUBLIC_KEY_SIZE, actual = keyPair.publicKey.size)
            assertEquals(expected = Identity.SECRET_KEY_SIZE, actual = keyPair.secretKey.size)
        } else {
            assertEquals(
                expected = "CryptoProviderFactory has not been wired to a platform crypto backend yet.",
                actual = result.exceptionOrNull()?.message,
                message = "Placeholder platforms should surface the missing Ed25519 backend explicitly",
            )
        }
    }

    @Test
    public fun x25519_returnsSharedSecretOrThrowsHelpfulPlaceholderError(): Unit {
        // Arrange
        val provider: CryptoProvider = CryptoProviderFactory.create()

        // Act
        val firstKeyPair: Result<KeyPair> = runCatching { provider.generateX25519KeyPair() }

        // Assert
        if (firstKeyPair.isSuccess) {
            val left: KeyPair = firstKeyPair.getOrThrow()
            val right: KeyPair = provider.generateX25519KeyPair()
            val leftSharedSecret: ByteArray = provider.x25519(
                privateKey = left.secretKey,
                publicKey = right.publicKey,
            )
            val rightSharedSecret: ByteArray = provider.x25519(
                privateKey = right.secretKey,
                publicKey = left.publicKey,
            )
            assertContentEquals(
                expected = leftSharedSecret,
                actual = rightSharedSecret,
                message = "Platform crypto providers should derive the same X25519 shared secret for both peers",
            )
        } else {
            val operation: Result<ByteArray> = runCatching {
                provider.x25519(privateKey = byteArrayOf(0x01), publicKey = byteArrayOf(0x02))
            }
            assertEquals(
                expected = "CryptoProviderFactory has not been wired to a platform crypto backend yet.",
                actual = operation.exceptionOrNull()?.message,
                message = "Placeholder platforms should surface the missing X25519 operation explicitly",
            )
        }
    }

    @Test
    public fun ed25519SignVerify_returnsTrueOrThrowsHelpfulPlaceholderError(): Unit {
        // Arrange
        val provider: CryptoProvider = CryptoProviderFactory.create()
        val message: ByteArray = "meshlink".encodeToByteArray()

        // Act
        val keyPairResult: Result<KeyPair> = runCatching { provider.generateEd25519KeyPair() }

        // Assert
        if (keyPairResult.isSuccess) {
            val keyPair: KeyPair = keyPairResult.getOrThrow()
            val signature: ByteArray = provider.ed25519Sign(
                privateKey = keyPair.secretKey,
                message = message,
            )
            val actual: Boolean = provider.ed25519Verify(
                publicKey = keyPair.publicKey,
                message = message,
                signature = signature,
            )
            assertTrue(
                actual = actual,
                message = "Platform crypto providers should verify Ed25519 signatures they generate",
            )
        } else {
            val operation: Result<ByteArray> = runCatching {
                provider.ed25519Sign(privateKey = byteArrayOf(0x01), message = byteArrayOf(0x02))
            }
            assertEquals(
                expected = "CryptoProviderFactory has not been wired to a platform crypto backend yet.",
                actual = operation.exceptionOrNull()?.message,
                message = "Placeholder platforms should surface the missing Ed25519 signing operation explicitly",
            )
        }
    }

    @Test
    public fun chaCha20Poly1305_roundTripsOrThrowsHelpfulPlaceholderError(): Unit {
        // Arrange
        val provider: CryptoProvider = CryptoProviderFactory.create()
        val key: ByteArray = ByteArray(size = 32) { index -> index.toByte() }
        val nonce: ByteArray = ByteArray(size = 12) { index -> (index + 1).toByte() }
        val aad: ByteArray = byteArrayOf(0x01, 0x02)
        val plaintext: ByteArray = byteArrayOf(0x11, 0x12, 0x13)

        // Act
        val result: Result<ByteArray> = runCatching {
            val ciphertext: ByteArray = provider.chaCha20Poly1305Encrypt(
                key = key,
                nonce = nonce,
                aad = aad,
                plaintext = plaintext,
            )
            provider.chaCha20Poly1305Decrypt(
                key = key,
                nonce = nonce,
                aad = aad,
                ciphertext = ciphertext,
            )
        }

        // Assert
        if (result.isSuccess) {
            assertContentEquals(
                expected = plaintext,
                actual = result.getOrThrow(),
                message = "Platform crypto providers should round-trip plaintext through ChaCha20-Poly1305 when available",
            )
        } else {
            assertEquals(
                expected = "CryptoProviderFactory has not been wired to a platform crypto backend yet.",
                actual = result.exceptionOrNull()?.message,
                message = "Placeholder platforms should surface the missing ChaCha20-Poly1305 backend explicitly",
            )
        }
    }

    @Test
    public fun hkdfSha256_returnsBytesOrThrowsHelpfulPlaceholderError(): Unit {
        // Arrange
        val provider: CryptoProvider = CryptoProviderFactory.create()

        // Act
        val result: Result<ByteArray> = runCatching {
            provider.hkdfSha256(
                ikm = byteArrayOf(0x01),
                salt = byteArrayOf(0x02),
                info = byteArrayOf(0x03),
                outputLength = 32,
            )
        }

        // Assert
        if (result.isSuccess) {
            assertEquals(
                expected = 32,
                actual = result.getOrThrow().size,
                message = "Platform crypto providers should produce HKDF output of the requested length",
            )
        } else {
            assertEquals(
                expected = "CryptoProviderFactory has not been wired to a platform crypto backend yet.",
                actual = result.exceptionOrNull()?.message,
                message = "Placeholder platforms should surface the missing HKDF backend explicitly",
            )
        }
    }

    @Test
    public fun hmacSha256_returnsBytesOrThrowsHelpfulPlaceholderError(): Unit {
        // Arrange
        val provider: CryptoProvider = CryptoProviderFactory.create()

        // Act
        val result: Result<ByteArray> = runCatching {
            provider.hmacSha256(key = byteArrayOf(0x01), message = byteArrayOf(0x02))
        }

        // Assert
        if (result.isSuccess) {
            assertTrue(
                actual = result.getOrThrow().isNotEmpty(),
                message = "Platform crypto providers should produce HMAC output when available",
            )
        } else {
            assertEquals(
                expected = "CryptoProviderFactory has not been wired to a platform crypto backend yet.",
                actual = result.exceptionOrNull()?.message,
                message = "Placeholder platforms should surface the missing HMAC backend explicitly",
            )
        }
    }
}
