package ch.trancee.meshlink.crypto.noise

/** Established Noise transport session with separate send and receive ciphers. */
public class NoiseSession(
  private val sendCipherState: CipherState,
  private val receiveCipherState: CipherState,
) {
  /** Encrypts an outbound transport message. */
  public fun seal(aad: ByteArray, plaintext: ByteArray): ByteArray {
    return sendCipherState.encryptWithAd(aad = aad, plaintext = plaintext)
  }

  /** Decrypts an inbound transport message. */
  public fun open(aad: ByteArray, ciphertext: ByteArray): ByteArray {
    return receiveCipherState.decryptWithAd(aad = aad, ciphertext = ciphertext)
  }
}
