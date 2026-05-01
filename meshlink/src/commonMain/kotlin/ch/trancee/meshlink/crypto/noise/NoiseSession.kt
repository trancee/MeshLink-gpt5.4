package ch.trancee.meshlink.crypto.noise

public class NoiseSession(
    private val sendCipherState: CipherState,
    private val receiveCipherState: CipherState,
) {
    public fun seal(
        aad: ByteArray,
        plaintext: ByteArray,
    ): ByteArray {
        return sendCipherState.encryptWithAd(
            aad = aad,
            plaintext = plaintext,
        )
    }

    public fun open(
        aad: ByteArray,
        ciphertext: ByteArray,
    ): ByteArray {
        return receiveCipherState.decryptWithAd(
            aad = aad,
            ciphertext = ciphertext,
        )
    }
}
