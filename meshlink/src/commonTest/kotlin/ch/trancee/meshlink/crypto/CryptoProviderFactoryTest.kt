package ch.trancee.meshlink.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

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
    public fun generateX25519KeyPair_throwsUntilPlatformBackendIsImplemented(): Unit {
        // Arrange
        val provider: CryptoProvider = CryptoProviderFactory.create()

        // Act
        val error = assertFailsWith<UnsupportedOperationException> {
            provider.generateX25519KeyPair()
        }

        // Assert
        assertEquals(
            expected = "CryptoProviderFactory has not been wired to a platform crypto backend yet.",
            actual = error.message,
            message = "Platform placeholder providers should make the missing X25519 backend explicit",
        )
    }

    @Test
    public fun generateEd25519KeyPair_throwsUntilPlatformBackendIsImplemented(): Unit {
        // Arrange
        val provider: CryptoProvider = CryptoProviderFactory.create()

        // Act
        val error = assertFailsWith<UnsupportedOperationException> {
            provider.generateEd25519KeyPair()
        }

        // Assert
        assertEquals(
            expected = "CryptoProviderFactory has not been wired to a platform crypto backend yet.",
            actual = error.message,
            message = "Platform placeholder providers should make the missing Ed25519 backend explicit",
        )
    }

    @Test
    public fun x25519_throwsUntilPlatformBackendIsImplemented(): Unit {
        // Arrange
        val provider: CryptoProvider = CryptoProviderFactory.create()

        // Act
        val error = assertFailsWith<UnsupportedOperationException> {
            provider.x25519(privateKey = byteArrayOf(0x01), publicKey = byteArrayOf(0x02))
        }

        // Assert
        assertEquals(
            expected = "CryptoProviderFactory has not been wired to a platform crypto backend yet.",
            actual = error.message,
            message = "Platform placeholder providers should make the missing X25519 operation explicit",
        )
    }

    @Test
    public fun ed25519Sign_throwsUntilPlatformBackendIsImplemented(): Unit {
        // Arrange
        val provider: CryptoProvider = CryptoProviderFactory.create()

        // Act
        val error = assertFailsWith<UnsupportedOperationException> {
            provider.ed25519Sign(privateKey = byteArrayOf(0x01), message = byteArrayOf(0x02))
        }

        // Assert
        assertEquals(
            expected = "CryptoProviderFactory has not been wired to a platform crypto backend yet.",
            actual = error.message,
            message = "Platform placeholder providers should make the missing Ed25519 signing operation explicit",
        )
    }

    @Test
    public fun ed25519Verify_throwsUntilPlatformBackendIsImplemented(): Unit {
        // Arrange
        val provider: CryptoProvider = CryptoProviderFactory.create()

        // Act
        val error = assertFailsWith<UnsupportedOperationException> {
            provider.ed25519Verify(
                publicKey = byteArrayOf(0x01),
                message = byteArrayOf(0x02),
                signature = byteArrayOf(0x03),
            )
        }

        // Assert
        assertEquals(
            expected = "CryptoProviderFactory has not been wired to a platform crypto backend yet.",
            actual = error.message,
            message = "Platform placeholder providers should make the missing Ed25519 verification operation explicit",
        )
    }

    @Test
    public fun chaCha20Poly1305Encrypt_throwsUntilPlatformBackendIsImplemented(): Unit {
        // Arrange
        val provider: CryptoProvider = CryptoProviderFactory.create()

        // Act
        val error = assertFailsWith<UnsupportedOperationException> {
            provider.chaCha20Poly1305Encrypt(
                key = byteArrayOf(0x01),
                nonce = byteArrayOf(0x02),
                aad = byteArrayOf(0x03),
                plaintext = byteArrayOf(0x04),
            )
        }

        // Assert
        assertEquals(
            expected = "CryptoProviderFactory has not been wired to a platform crypto backend yet.",
            actual = error.message,
            message = "Platform placeholder providers should make the missing ChaCha20-Poly1305 encryption operation explicit",
        )
    }

    @Test
    public fun chaCha20Poly1305Decrypt_throwsUntilPlatformBackendIsImplemented(): Unit {
        // Arrange
        val provider: CryptoProvider = CryptoProviderFactory.create()

        // Act
        val error = assertFailsWith<UnsupportedOperationException> {
            provider.chaCha20Poly1305Decrypt(
                key = byteArrayOf(0x01),
                nonce = byteArrayOf(0x02),
                aad = byteArrayOf(0x03),
                ciphertext = byteArrayOf(0x04),
            )
        }

        // Assert
        assertEquals(
            expected = "CryptoProviderFactory has not been wired to a platform crypto backend yet.",
            actual = error.message,
            message = "Platform placeholder providers should make the missing ChaCha20-Poly1305 decryption operation explicit",
        )
    }

    @Test
    public fun hkdfSha256_throwsUntilPlatformBackendIsImplemented(): Unit {
        // Arrange
        val provider: CryptoProvider = CryptoProviderFactory.create()

        // Act
        val error = assertFailsWith<UnsupportedOperationException> {
            provider.hkdfSha256(
                ikm = byteArrayOf(0x01),
                salt = byteArrayOf(0x02),
                info = byteArrayOf(0x03),
                outputLength = 32,
            )
        }

        // Assert
        assertEquals(
            expected = "CryptoProviderFactory has not been wired to a platform crypto backend yet.",
            actual = error.message,
            message = "Platform placeholder providers should make the missing HKDF operation explicit",
        )
    }

    @Test
    public fun hmacSha256_throwsUntilPlatformBackendIsImplemented(): Unit {
        // Arrange
        val provider: CryptoProvider = CryptoProviderFactory.create()

        // Act
        val error = assertFailsWith<UnsupportedOperationException> {
            provider.hmacSha256(key = byteArrayOf(0x01), message = byteArrayOf(0x02))
        }

        // Assert
        assertEquals(
            expected = "CryptoProviderFactory has not been wired to a platform crypto backend yet.",
            actual = error.message,
            message = "Platform placeholder providers should make the missing HMAC operation explicit",
        )
    }
}
