package ch.trancee.meshlink.crypto

public data class RotationAnnouncement(
  public val previousPublicKey: ByteArray,
  public val nextPublicKey: ByteArray,
  public val signature: ByteArray,
) {
  public fun verify(provider: CryptoProvider): Boolean {
    validatePublicKey(name = "previousPublicKey", value = previousPublicKey)
    validatePublicKey(name = "nextPublicKey", value = nextPublicKey)
    validateSignature(signature = signature)
    return provider.ed25519Verify(
      publicKey = previousPublicKey,
      message =
        signingPayload(previousPublicKey = previousPublicKey, nextPublicKey = nextPublicKey),
      signature = signature,
    )
  }

  public companion object {
    public const val SIGNATURE_SIZE: Int = 64

    public fun create(
      provider: CryptoProvider,
      previousIdentity: Identity,
      nextPublicKey: ByteArray,
    ): RotationAnnouncement {
      validatePublicKey(name = "previousPublicKey", value = previousIdentity.publicKey)
      validateSecretKey(secretKey = previousIdentity.secretKey)
      validatePublicKey(name = "nextPublicKey", value = nextPublicKey)

      val payload: ByteArray =
        signingPayload(
          previousPublicKey = previousIdentity.publicKey,
          nextPublicKey = nextPublicKey,
        )
      val signature: ByteArray =
        provider.ed25519Sign(privateKey = previousIdentity.secretKey, message = payload)
      validateSignature(signature = signature)

      return RotationAnnouncement(
        previousPublicKey = previousIdentity.publicKey.copyOf(),
        nextPublicKey = nextPublicKey.copyOf(),
        signature = signature.copyOf(),
      )
    }

    private fun validatePublicKey(name: String, value: ByteArray): Unit {
      if (value.size != Identity.PUBLIC_KEY_SIZE) {
        throw IllegalArgumentException(
          "RotationAnnouncement $name must be exactly ${Identity.PUBLIC_KEY_SIZE} bytes."
        )
      }
    }

    private fun validateSecretKey(secretKey: ByteArray): Unit {
      if (secretKey.size != Identity.SECRET_KEY_SIZE) {
        throw IllegalArgumentException(
          "RotationAnnouncement previous secretKey must be exactly ${Identity.SECRET_KEY_SIZE} bytes."
        )
      }
    }

    private fun validateSignature(signature: ByteArray): Unit {
      if (signature.size != SIGNATURE_SIZE) {
        throw IllegalArgumentException(
          "RotationAnnouncement signature must be exactly $SIGNATURE_SIZE bytes."
        )
      }
    }

    private fun signingPayload(previousPublicKey: ByteArray, nextPublicKey: ByteArray): ByteArray {
      return previousPublicKey + nextPublicKey
    }
  }
}
