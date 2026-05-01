package ch.trancee.meshlink.crypto.noise

import ch.trancee.meshlink.crypto.CryptoProvider
import ch.trancee.meshlink.crypto.KeyPair
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

public class SymmetricStateTest {
    @Test
    public fun hasCipherKey_returnsFalseBeforeMixKeyRuns(): Unit {
        // Arrange
        val state = SymmetricState(
            provider = FakeNoiseCryptoProvider(),
            initialChainingKey = byteArrayOf(0x01),
            initialHandshakeHash = byteArrayOf(0x02),
        )

        // Act
        val actual: Boolean = state.hasCipherKey()

        // Assert
        assertFalse(
            actual = actual,
            message = "SymmetricState should report that no cipher key exists before key material has been mixed",
        )
    }

    @Test
    public fun mixHash_updatesHandshakeHashUsingHmacSha256(): Unit {
        // Arrange
        val provider = FakeNoiseCryptoProvider()
        val state = SymmetricState(
            provider = provider,
            initialChainingKey = byteArrayOf(0x01),
            initialHandshakeHash = byteArrayOf(0x02),
        )
        provider.nextHmacResult = byteArrayOf(0x11, 0x12)

        // Act
        val actual: ByteArray = state.mixHash(data = byteArrayOf(0x21, 0x22))

        // Assert
        assertContentEquals(
            expected = byteArrayOf(0x11, 0x12),
            actual = actual,
            message = "SymmetricState should replace the handshake hash with the HMAC result when mixing hash data",
        )
        assertContentEquals(
            expected = byteArrayOf(0x02),
            actual = provider.lastHmacKey,
            message = "SymmetricState should use the previous handshake hash as the HMAC key",
        )
        assertContentEquals(
            expected = byteArrayOf(0x21, 0x22),
            actual = provider.lastHmacMessage,
            message = "SymmetricState should hash the supplied transcript bytes",
        )
    }

    @Test
    public fun mixKey_updatesChainingKeyAndEnablesCipherKey(): Unit {
        // Arrange
        val provider = FakeNoiseCryptoProvider()
        val state = SymmetricState(
            provider = provider,
            initialChainingKey = byteArrayOf(0x31, 0x32),
            initialHandshakeHash = byteArrayOf(0x41),
        )
        provider.nextHkdfResult = ByteArray(size = 64) { index -> index.toByte() }

        // Act
        val actualCipherKey: ByteArray = state.mixKey(inputKeyMaterial = byteArrayOf(0x51, 0x52))

        // Assert
        assertTrue(
            actual = state.hasCipherKey(),
            message = "SymmetricState should mark the cipher key as initialized after mixing key material",
        )
        assertContentEquals(
            expected = ByteArray(size = 32) { index -> index.toByte() },
            actual = state.chainingKey(),
            message = "SymmetricState should take the first half of HKDF output as the new chaining key",
        )
        assertContentEquals(
            expected = ByteArray(size = 32) { index -> (index + 32).toByte() },
            actual = actualCipherKey,
            message = "SymmetricState should take the second half of HKDF output as the new cipher key",
        )
        assertContentEquals(
            expected = byteArrayOf(0x31, 0x32),
            actual = provider.lastHkdfSalt,
            message = "SymmetricState should use the previous chaining key as HKDF salt",
        )
        assertContentEquals(
            expected = byteArrayOf(0x51, 0x52),
            actual = provider.lastHkdfIkm,
            message = "SymmetricState should use the input key material as HKDF IKM",
        )
    }

    @Test
    public fun encryptAndHash_returnsPlaintextWhenCipherKeyIsAbsent(): Unit {
        // Arrange
        val provider = FakeNoiseCryptoProvider()
        provider.nextHmacResult = byteArrayOf(0x71)
        val state = SymmetricState(
            provider = provider,
            initialChainingKey = byteArrayOf(0x61),
            initialHandshakeHash = byteArrayOf(0x62),
        )
        val plaintext: ByteArray = byteArrayOf(0x01, 0x02)

        // Act
        val actualCiphertext: ByteArray = state.encryptAndHash(plaintext = plaintext)

        // Assert
        assertContentEquals(
            expected = plaintext,
            actual = actualCiphertext,
            message = "SymmetricState should echo plaintext when no cipher key has been mixed yet",
        )
        assertContentEquals(
            expected = plaintext,
            actual = provider.lastHmacMessage,
            message = "SymmetricState should mix the plaintext into the handshake hash when no cipher key is present",
        )
    }

    @Test
    public fun encryptAndHash_usesCipherKeyAndCurrentHandshakeHashAsAad(): Unit {
        // Arrange
        val provider = FakeNoiseCryptoProvider()
        provider.nextHkdfResult = ByteArray(size = 64) { index -> (index + 1).toByte() }
        provider.nextEncryptResult = byteArrayOf(0x55, 0x56, 0x57)
        provider.nextHmacResult = byteArrayOf(0x41, 0x42)
        val state = SymmetricState(
            provider = provider,
            initialChainingKey = byteArrayOf(0x11),
            initialHandshakeHash = byteArrayOf(0x21, 0x22),
        )
        state.mixKey(inputKeyMaterial = byteArrayOf(0x31))

        // Act
        val actualCiphertext: ByteArray = state.encryptAndHash(plaintext = byteArrayOf(0x61, 0x62))

        // Assert
        assertContentEquals(
            expected = byteArrayOf(0x55, 0x56, 0x57),
            actual = actualCiphertext,
            message = "SymmetricState should return the provider-produced ciphertext once a cipher key exists",
        )
        assertContentEquals(
            expected = ByteArray(size = 32) { index -> (index + 33).toByte() },
            actual = provider.lastEncryptKey,
            message = "SymmetricState should encrypt with the mixed cipher key",
        )
        assertContentEquals(
            expected = byteArrayOf(0x21, 0x22),
            actual = provider.lastEncryptAad,
            message = "SymmetricState should authenticate ciphertext against the current handshake hash",
        )
        assertContentEquals(
            expected = byteArrayOf(0x55, 0x56, 0x57),
            actual = provider.lastHmacMessage,
            message = "SymmetricState should mix ciphertext into the handshake hash after encryption",
        )
    }

    @Test
    public fun decryptAndHash_returnsCiphertextWhenCipherKeyIsAbsent(): Unit {
        // Arrange
        val provider = FakeNoiseCryptoProvider()
        provider.nextHmacResult = byteArrayOf(0x21)
        val state = SymmetricState(
            provider = provider,
            initialChainingKey = byteArrayOf(0x41),
            initialHandshakeHash = byteArrayOf(0x42),
        )
        val ciphertext: ByteArray = byteArrayOf(0x11, 0x12)

        // Act
        val actualPlaintext: ByteArray = state.decryptAndHash(ciphertext = ciphertext)

        // Assert
        assertContentEquals(
            expected = ciphertext,
            actual = actualPlaintext,
            message = "SymmetricState should echo ciphertext when no cipher key has been mixed yet",
        )
        assertContentEquals(
            expected = ciphertext,
            actual = provider.lastHmacMessage,
            message = "SymmetricState should mix ciphertext into the handshake hash when decrypting without a cipher key",
        )
    }

    @Test
    public fun decryptAndHash_usesCipherKeyAndCurrentHandshakeHashAsAad(): Unit {
        // Arrange
        val provider = FakeNoiseCryptoProvider()
        provider.nextHkdfResult = ByteArray(size = 64) { index -> (index + 1).toByte() }
        provider.nextDecryptResult = byteArrayOf(0x66, 0x67)
        provider.nextHmacResult = byteArrayOf(0x31, 0x32)
        val state = SymmetricState(
            provider = provider,
            initialChainingKey = byteArrayOf(0x51),
            initialHandshakeHash = byteArrayOf(0x52, 0x53),
        )
        state.mixKey(inputKeyMaterial = byteArrayOf(0x61))

        // Act
        val actualPlaintext: ByteArray = state.decryptAndHash(ciphertext = byteArrayOf(0x71, 0x72, 0x73))

        // Assert
        assertContentEquals(
            expected = byteArrayOf(0x66, 0x67),
            actual = actualPlaintext,
            message = "SymmetricState should return the provider-produced plaintext once a cipher key exists",
        )
        assertContentEquals(
            expected = ByteArray(size = 32) { index -> (index + 33).toByte() },
            actual = provider.lastDecryptKey,
            message = "SymmetricState should decrypt with the mixed cipher key",
        )
        assertContentEquals(
            expected = byteArrayOf(0x52, 0x53),
            actual = provider.lastDecryptAad,
            message = "SymmetricState should authenticate ciphertext against the current handshake hash during decrypt",
        )
        assertContentEquals(
            expected = byteArrayOf(0x71, 0x72, 0x73),
            actual = provider.lastHmacMessage,
            message = "SymmetricState should mix ciphertext into the handshake hash after decryption",
        )
    }

    private class FakeNoiseCryptoProvider : CryptoProvider {
        var nextEncryptResult: ByteArray = byteArrayOf()
        var nextDecryptResult: ByteArray = byteArrayOf()
        var nextHkdfResult: ByteArray = ByteArray(size = 64)
        var nextHmacResult: ByteArray = byteArrayOf()
        var lastEncryptKey: ByteArray = byteArrayOf()
        var lastEncryptAad: ByteArray = byteArrayOf()
        var lastDecryptKey: ByteArray = byteArrayOf()
        var lastDecryptAad: ByteArray = byteArrayOf()
        var lastHkdfSalt: ByteArray = byteArrayOf()
        var lastHkdfIkm: ByteArray = byteArrayOf()
        var lastHmacKey: ByteArray = byteArrayOf()
        var lastHmacMessage: ByteArray = byteArrayOf()

        override fun generateX25519KeyPair(): KeyPair = unsupported()

        override fun generateEd25519KeyPair(): KeyPair = unsupported()

        override fun x25519(privateKey: ByteArray, publicKey: ByteArray): ByteArray = unsupported()

        override fun ed25519Sign(privateKey: ByteArray, message: ByteArray): ByteArray = unsupported()

        override fun ed25519Verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean = unsupported()

        override fun chaCha20Poly1305Encrypt(key: ByteArray, nonce: ByteArray, aad: ByteArray, plaintext: ByteArray): ByteArray {
            lastEncryptKey = key.copyOf()
            lastEncryptAad = aad.copyOf()
            return nextEncryptResult.copyOf()
        }

        override fun chaCha20Poly1305Decrypt(key: ByteArray, nonce: ByteArray, aad: ByteArray, ciphertext: ByteArray): ByteArray {
            lastDecryptKey = key.copyOf()
            lastDecryptAad = aad.copyOf()
            return nextDecryptResult.copyOf()
        }

        override fun hkdfSha256(ikm: ByteArray, salt: ByteArray, info: ByteArray, outputLength: Int): ByteArray {
            lastHkdfIkm = ikm.copyOf()
            lastHkdfSalt = salt.copyOf()
            return nextHkdfResult.copyOf()
        }

        override fun hmacSha256(key: ByteArray, message: ByteArray): ByteArray {
            lastHmacKey = key.copyOf()
            lastHmacMessage = message.copyOf()
            return nextHmacResult.copyOf()
        }

        private fun <T> unsupported(): T {
            throw UnsupportedOperationException("not used in test")
        }
    }
}
