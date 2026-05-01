package ch.trancee.meshlink.crypto

/** Stable signing identity used by the local node. */
public data class Identity(
  public val publicKey: ByteArray,
  public val secretKey: ByteArray,
  public val keyHash: ByteArray,
) {
  public companion object {
    public const val PUBLIC_KEY_SIZE: Int = 32
    public const val SECRET_KEY_SIZE: Int = 64
    private val KEY_HASH_CONTEXT: ByteArray = "meshlink-identity-key-hash".encodeToByteArray()

    /** Generates a fresh Ed25519 identity. */
    public fun generate(provider: CryptoProvider = CryptoProviderFactory.create()): Identity {
      val keyPair: KeyPair = provider.generateEd25519KeyPair()
      return fromKeyPair(provider = provider, keyPair = keyPair)
    }

    /** Builds an identity from an existing Ed25519 key pair. */
    public fun fromKeyPair(provider: CryptoProvider, keyPair: KeyPair): Identity {
      if (keyPair.publicKey.size != PUBLIC_KEY_SIZE) {
        throw IllegalArgumentException("Identity publicKey must be exactly $PUBLIC_KEY_SIZE bytes.")
      }
      if (keyPair.secretKey.size != SECRET_KEY_SIZE) {
        throw IllegalArgumentException("Identity secretKey must be exactly $SECRET_KEY_SIZE bytes.")
      }

      val keyHash: ByteArray =
        provider.hmacSha256(key = keyPair.publicKey, message = KEY_HASH_CONTEXT)
      return Identity(
        publicKey = keyPair.publicKey.copyOf(),
        secretKey = keyPair.secretKey.copyOf(),
        keyHash = keyHash,
      )
    }
  }
}
