package ch.trancee.meshlink.crypto.noise

/** Convenience wrapper for sealing payloads with an established Noise cipher state. */
public object NoiseKSeal {
  public fun seal(cipherState: CipherState, aad: ByteArray, plaintext: ByteArray): ByteArray {
    return cipherState.encryptWithAd(aad = aad, plaintext = plaintext)
  }
}
