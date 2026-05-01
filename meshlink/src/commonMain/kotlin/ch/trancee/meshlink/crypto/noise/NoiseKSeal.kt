package ch.trancee.meshlink.crypto.noise

public object NoiseKSeal {
    public fun seal(
        cipherState: CipherState,
        aad: ByteArray,
        plaintext: ByteArray,
    ): ByteArray {
        return cipherState.encryptWithAd(
            aad = aad,
            plaintext = plaintext,
        )
    }
}
