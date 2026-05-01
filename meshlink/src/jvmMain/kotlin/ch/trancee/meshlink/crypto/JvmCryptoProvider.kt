package ch.trancee.meshlink.crypto

import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

public class JvmCryptoProvider : CryptoProvider {
    override fun generateX25519KeyPair(): KeyPair = unsupported()

    override fun generateEd25519KeyPair(): KeyPair = unsupported()

    override fun x25519(
        privateKey: ByteArray,
        publicKey: ByteArray,
    ): ByteArray = unsupported()

    override fun ed25519Sign(
        privateKey: ByteArray,
        message: ByteArray,
    ): ByteArray = unsupported()

    override fun ed25519Verify(
        publicKey: ByteArray,
        message: ByteArray,
        signature: ByteArray,
    ): Boolean = unsupported()

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
    ): ByteArray {
        require(outputLength >= 0) { "HKDF outputLength must be non-negative." }
        val pseudoRandomKey: ByteArray = hmacSha256(
            key = if (salt.isEmpty()) ByteArray(size = HASH_OUTPUT_SIZE) else salt,
            message = ikm,
        )
        val output = ByteArray(size = outputLength)
        var previousBlock = ByteArray(size = 0)
        var generatedBytes: Int = 0
        var counter: Int = 1

        while (generatedBytes < outputLength) {
            val input = ByteArray(size = previousBlock.size + info.size + 1)
            previousBlock.copyInto(destination = input, destinationOffset = 0)
            info.copyInto(destination = input, destinationOffset = previousBlock.size)
            input[input.lastIndex] = counter.toByte()
            previousBlock = hmacSha256(key = pseudoRandomKey, message = input)
            val bytesToCopy: Int = minOf(previousBlock.size, outputLength - generatedBytes)
            previousBlock.copyInto(destination = output, destinationOffset = generatedBytes, endIndex = bytesToCopy)
            generatedBytes += bytesToCopy
            counter += 1
        }

        return output
    }

    override fun hmacSha256(
        key: ByteArray,
        message: ByteArray,
    ): ByteArray {
        val mac: Mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(message)
    }

    private fun <T> unsupported(): T {
        throw UnsupportedOperationException("JvmCryptoProvider primitive is not implemented yet.")
    }

    public companion object {
        private const val HASH_OUTPUT_SIZE: Int = 32
    }
}
