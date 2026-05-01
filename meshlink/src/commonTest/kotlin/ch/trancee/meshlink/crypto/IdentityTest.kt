package ch.trancee.meshlink.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

public class IdentityTest {
    @Test
    public fun generate_usesProviderKeyPairAndDerivesKeyHash(): Unit {
        // Arrange
        val expectedPublicKey: ByteArray = ByteArray(size = Identity.PUBLIC_KEY_SIZE) { index -> (index + 1).toByte() }
        val expectedSecretKey: ByteArray = ByteArray(size = Identity.SECRET_KEY_SIZE) { index -> (index + 33).toByte() }
        val expectedKeyHash: ByteArray = byteArrayOf(0x61, 0x62, 0x63)
        val provider = FakeCryptoProvider(
            generatedEd25519KeyPair = KeyPair(
                publicKey = expectedPublicKey,
                secretKey = expectedSecretKey,
            ),
            hmacResult = expectedKeyHash,
        )

        // Act
        val actual: Identity = Identity.generate(provider = provider)

        // Assert
        assertContentEquals(
            expected = expectedPublicKey,
            actual = actual.publicKey,
            message = "Identity.generate should retain the provider-generated public key",
        )
        assertContentEquals(
            expected = expectedSecretKey,
            actual = actual.secretKey,
            message = "Identity.generate should retain the provider-generated secret key",
        )
        assertContentEquals(
            expected = expectedKeyHash,
            actual = actual.keyHash,
            message = "Identity.generate should derive the key hash through the CryptoProvider",
        )
        assertEquals(
            expected = 1,
            actual = provider.generateEd25519Calls,
            message = "Identity.generate should request exactly one Ed25519 key pair from the provider",
        )
        assertEquals(
            expected = 1,
            actual = provider.hmacCalls,
            message = "Identity.generate should derive exactly one key hash",
        )
    }

    @Test
    public fun generate_usesDefaultProviderWhenAvailableOrThrowsHelpfulPlaceholderError(): Unit {
        // Arrange
        
        // Act
        val result: Result<Identity> = runCatching { Identity.generate() }

        // Assert
        if (result.isSuccess) {
            val identity: Identity = result.getOrThrow()
            assertEquals(expected = Identity.PUBLIC_KEY_SIZE, actual = identity.publicKey.size)
            assertEquals(expected = Identity.SECRET_KEY_SIZE, actual = identity.secretKey.size)
            assertTrue(actual = identity.keyHash.isNotEmpty())
        } else {
            assertEquals(
                expected = "CryptoProviderFactory has not been wired to a platform crypto backend yet.",
                actual = result.exceptionOrNull()?.message,
                message = "Identity.generate should surface the missing platform crypto backend when the current platform has no provider",
            )
        }
    }

    @Test
    public fun fromKeyPair_throwsWhenPublicKeyLengthIsInvalid(): Unit {
        // Arrange
        val provider = FakeCryptoProvider(
            generatedEd25519KeyPair = KeyPair(publicKey = ByteArray(size = Identity.PUBLIC_KEY_SIZE), secretKey = ByteArray(size = Identity.SECRET_KEY_SIZE)),
            hmacResult = byteArrayOf(0x01),
        )
        val keyPair = KeyPair(
            publicKey = byteArrayOf(0x01),
            secretKey = ByteArray(size = Identity.SECRET_KEY_SIZE),
        )

        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            Identity.fromKeyPair(provider = provider, keyPair = keyPair)
        }

        // Assert
        assertEquals(
            expected = "Identity publicKey must be exactly 32 bytes.",
            actual = error.message,
            message = "Identity should reject malformed Ed25519 public keys",
        )
    }

    @Test
    public fun fromKeyPair_throwsWhenSecretKeyLengthIsInvalid(): Unit {
        // Arrange
        val provider = FakeCryptoProvider(
            generatedEd25519KeyPair = KeyPair(publicKey = ByteArray(size = Identity.PUBLIC_KEY_SIZE), secretKey = ByteArray(size = Identity.SECRET_KEY_SIZE)),
            hmacResult = byteArrayOf(0x01),
        )
        val keyPair = KeyPair(
            publicKey = ByteArray(size = Identity.PUBLIC_KEY_SIZE),
            secretKey = byteArrayOf(0x01),
        )

        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            Identity.fromKeyPair(provider = provider, keyPair = keyPair)
        }

        // Assert
        assertEquals(
            expected = "Identity secretKey must be exactly 64 bytes.",
            actual = error.message,
            message = "Identity should reject malformed Ed25519 secret keys",
        )
    }

    private class FakeCryptoProvider(
        private val generatedEd25519KeyPair: KeyPair,
        private val hmacResult: ByteArray,
    ) : CryptoProvider {
        var generateEd25519Calls: Int = 0
            private set
        var hmacCalls: Int = 0
            private set

        override fun generateX25519KeyPair(): KeyPair {
            throw UnsupportedOperationException("not used in test")
        }

        override fun generateEd25519KeyPair(): KeyPair {
            generateEd25519Calls += 1
            return generatedEd25519KeyPair
        }

        override fun x25519(privateKey: ByteArray, publicKey: ByteArray): ByteArray {
            throw UnsupportedOperationException("not used in test")
        }

        override fun ed25519Sign(privateKey: ByteArray, message: ByteArray): ByteArray {
            throw UnsupportedOperationException("not used in test")
        }

        override fun ed25519Verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean {
            throw UnsupportedOperationException("not used in test")
        }

        override fun chaCha20Poly1305Encrypt(
            key: ByteArray,
            nonce: ByteArray,
            aad: ByteArray,
            plaintext: ByteArray,
        ): ByteArray {
            throw UnsupportedOperationException("not used in test")
        }

        override fun chaCha20Poly1305Decrypt(
            key: ByteArray,
            nonce: ByteArray,
            aad: ByteArray,
            ciphertext: ByteArray,
        ): ByteArray {
            throw UnsupportedOperationException("not used in test")
        }

        override fun hkdfSha256(
            ikm: ByteArray,
            salt: ByteArray,
            info: ByteArray,
            outputLength: Int,
        ): ByteArray {
            throw UnsupportedOperationException("not used in test")
        }

        override fun hmacSha256(key: ByteArray, message: ByteArray): ByteArray {
            hmacCalls += 1
            return hmacResult
        }
    }
}
