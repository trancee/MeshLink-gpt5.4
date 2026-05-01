package ch.trancee.meshlink.crypto.noise

/** Convenience wrapper for opening payloads with an established Noise cipher state. */
public object NoiseKOpen {
  public fun open(cipherState: CipherState, aad: ByteArray, ciphertext: ByteArray): ByteArray {
    return cipherState.decryptWithAd(aad = aad, ciphertext = ciphertext)
  }
}
