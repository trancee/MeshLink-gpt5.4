package ch.trancee.meshlink.crypto.noise

import ch.trancee.meshlink.crypto.CryptoProvider
import ch.trancee.meshlink.crypto.KeyPair
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

public class NoiseSessionTest {
    @Test
    public fun seal_usesSendCipherState(): Unit {
        // Arrange
        val sendProvider = FakeSessionCryptoProvider().apply {
            nextEncryptResult = byteArrayOf(0x41, 0x42)
        }
        val receiveProvider = FakeSessionCryptoProvider()
        val sendCipherState = CipherState(provider = sendProvider).apply {
            initializeKey(key = ByteArray(size = CipherState.KEY_SIZE) { index -> (index + 1).toByte() })
        }
        val receiveCipherState = CipherState(provider = receiveProvider).apply {
            initializeKey(key = ByteArray(size = CipherState.KEY_SIZE) { index -> (index + 33).toByte() })
        }
        val session = NoiseSession(
            sendCipherState = sendCipherState,
            receiveCipherState = receiveCipherState,
        )

        // Act
        val actual: ByteArray = session.seal(aad = byteArrayOf(0x11), plaintext = byteArrayOf(0x21, 0x22))

        // Assert
        assertContentEquals(
            expected = byteArrayOf(0x41, 0x42),
            actual = actual,
            message = "NoiseSession should delegate sealing to the send cipher state",
        )
        assertEquals(
            expected = 1u,
            actual = sendCipherState.nonce(),
            message = "NoiseSession should advance the send nonce when sealing",
        )
        assertEquals(
            expected = 0u,
            actual = receiveCipherState.nonce(),
            message = "NoiseSession should not advance the receive nonce when sealing",
        )
    }

    @Test
    public fun open_usesReceiveCipherState(): Unit {
        // Arrange
        val sendProvider = FakeSessionCryptoProvider()
        val receiveProvider = FakeSessionCryptoProvider().apply {
            nextDecryptResult = byteArrayOf(0x61, 0x62)
        }
        val sendCipherState = CipherState(provider = sendProvider).apply {
            initializeKey(key = ByteArray(size = CipherState.KEY_SIZE) { index -> (index + 1).toByte() })
        }
        val receiveCipherState = CipherState(provider = receiveProvider).apply {
            initializeKey(key = ByteArray(size = CipherState.KEY_SIZE) { index -> (index + 33).toByte() })
        }
        val session = NoiseSession(
            sendCipherState = sendCipherState,
            receiveCipherState = receiveCipherState,
        )

        // Act
        val actual: ByteArray = session.open(aad = byteArrayOf(0x31), ciphertext = byteArrayOf(0x71, 0x72))

        // Assert
        assertContentEquals(
            expected = byteArrayOf(0x61, 0x62),
            actual = actual,
            message = "NoiseSession should delegate opening to the receive cipher state",
        )
        assertEquals(
            expected = 0u,
            actual = sendCipherState.nonce(),
            message = "NoiseSession should not advance the send nonce when opening",
        )
        assertEquals(
            expected = 1u,
            actual = receiveCipherState.nonce(),
            message = "NoiseSession should advance the receive nonce when opening",
        )
    }

    private class FakeSessionCryptoProvider : CryptoProvider {
        var nextEncryptResult: ByteArray = byteArrayOf()
        var nextDecryptResult: ByteArray = byteArrayOf()

        override fun generateX25519KeyPair(): KeyPair = unsupported()
        override fun generateEd25519KeyPair(): KeyPair = unsupported()
        override fun x25519(privateKey: ByteArray, publicKey: ByteArray): ByteArray = unsupported()
        override fun ed25519Sign(privateKey: ByteArray, message: ByteArray): ByteArray = unsupported()
        override fun ed25519Verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean = unsupported()
        override fun chaCha20Poly1305Encrypt(key: ByteArray, nonce: ByteArray, aad: ByteArray, plaintext: ByteArray): ByteArray = nextEncryptResult.copyOf()
        override fun chaCha20Poly1305Decrypt(key: ByteArray, nonce: ByteArray, aad: ByteArray, ciphertext: ByteArray): ByteArray = nextDecryptResult.copyOf()
        override fun hkdfSha256(ikm: ByteArray, salt: ByteArray, info: ByteArray, outputLength: Int): ByteArray = unsupported()
        override fun hmacSha256(key: ByteArray, message: ByteArray): ByteArray = unsupported()

        private fun <T> unsupported(): T {
            throw UnsupportedOperationException("not used in test")
        }
    }
}
