package ch.trancee.meshlink.crypto

import kotlinx.cinterop.ExperimentalForeignApi

private const val IOS_CHACHA20_KEY_SIZE: Int = 32
private const val IOS_CHACHA20_NONCE_SIZE: Int = 12
private const val IOS_X25519_KEY_SIZE: Int = 32
private const val ED25519_SIGNATURE_SIZE: Int = 64
private const val HMAC_SHA256_SIZE: Int = 32

@OptIn(ExperimentalForeignApi::class)
public class IosCryptoProvider : CryptoProvider {
  override fun generateX25519KeyPair(): KeyPair {
    val keyPair: IosCryptoKeyPair =
      requireNotNull(currentDelegate().generateX25519KeyPair()) {
        "IosCryptoProvider delegate failed to generate an X25519 key pair."
      }
    return KeyPair(
      publicKey =
        CryptoOutputValidator.requireExactSize(
          bytes = keyPair.publicKey.toByteArray(),
          expectedSize = IOS_X25519_KEY_SIZE,
          label = "X25519 publicKey",
        ),
      secretKey =
        CryptoOutputValidator.requireExactSize(
          bytes = keyPair.secretKey.toByteArray(),
          expectedSize = IOS_X25519_KEY_SIZE,
          label = "X25519 secretKey",
        ),
    )
  }

  override fun generateEd25519KeyPair(): KeyPair {
    val keyPair: IosCryptoKeyPair =
      requireNotNull(currentDelegate().generateEd25519KeyPair()) {
        "IosCryptoProvider delegate failed to generate an Ed25519 key pair."
      }
    return KeyPair(
      publicKey =
        CryptoOutputValidator.requireExactSize(
          bytes = keyPair.publicKey.toByteArray(),
          expectedSize = Identity.PUBLIC_KEY_SIZE,
          label = "Ed25519 publicKey",
        ),
      secretKey =
        CryptoOutputValidator.requireExactSize(
          bytes = keyPair.secretKey.toByteArray(),
          expectedSize = Identity.SECRET_KEY_SIZE,
          label = "Ed25519 secretKey",
        ),
    )
  }

  override fun x25519(privateKey: ByteArray, publicKey: ByteArray): ByteArray {
    require(privateKey.size == IOS_X25519_KEY_SIZE) {
      "X25519 privateKey must be exactly $IOS_X25519_KEY_SIZE bytes."
    }
    require(publicKey.size == IOS_X25519_KEY_SIZE) {
      "X25519 publicKey must be exactly $IOS_X25519_KEY_SIZE bytes."
    }
    return CryptoOutputValidator.requireExactSize(
      bytes =
        requireNotNull(
            currentDelegate()
              .x25519(privateKey = privateKey.toNSData(), publicKey = publicKey.toNSData())
          ) {
            "IosCryptoProvider delegate failed to compute an X25519 shared secret."
          }
          .toByteArray(),
      expectedSize = IOS_X25519_KEY_SIZE,
      label = "X25519 sharedSecret",
    )
  }

  override fun ed25519Sign(privateKey: ByteArray, message: ByteArray): ByteArray {
    require(privateKey.size == Identity.SECRET_KEY_SIZE) {
      "Ed25519 secretKey must be exactly ${Identity.SECRET_KEY_SIZE} bytes."
    }
    return CryptoOutputValidator.requireExactSize(
      bytes =
        requireNotNull(
            currentDelegate()
              .ed25519Sign(privateKey = privateKey.toNSData(), message = message.toNSData())
          ) {
            "IosCryptoProvider delegate failed to sign an Ed25519 message."
          }
          .toByteArray(),
      expectedSize = ED25519_SIGNATURE_SIZE,
      label = "Ed25519 signature",
    )
  }

  override fun ed25519Verify(
    publicKey: ByteArray,
    message: ByteArray,
    signature: ByteArray,
  ): Boolean {
    require(publicKey.size == Identity.PUBLIC_KEY_SIZE) {
      "Ed25519 publicKey must be exactly ${Identity.PUBLIC_KEY_SIZE} bytes."
    }
    return currentDelegate()
      .ed25519Verify(
        publicKey = publicKey.toNSData(),
        message = message.toNSData(),
        signature = signature.toNSData(),
      )
  }

  override fun chaCha20Poly1305Encrypt(
    key: ByteArray,
    nonce: ByteArray,
    aad: ByteArray,
    plaintext: ByteArray,
  ): ByteArray {
    require(key.size == IOS_CHACHA20_KEY_SIZE) {
      "ChaCha20-Poly1305 key must be exactly $IOS_CHACHA20_KEY_SIZE bytes."
    }
    require(nonce.size == IOS_CHACHA20_NONCE_SIZE) {
      "ChaCha20-Poly1305 nonce must be exactly $IOS_CHACHA20_NONCE_SIZE bytes."
    }
    return requireNotNull(
        currentDelegate()
          .chaCha20Poly1305Encrypt(
            key = key.toNSData(),
            nonce = nonce.toNSData(),
            aad = aad.toNSData(),
            plaintext = plaintext.toNSData(),
          )
      ) {
        "IosCryptoProvider delegate failed to encrypt a ChaCha20-Poly1305 payload."
      }
      .toByteArray()
  }

  override fun chaCha20Poly1305Decrypt(
    key: ByteArray,
    nonce: ByteArray,
    aad: ByteArray,
    ciphertext: ByteArray,
  ): ByteArray {
    require(key.size == IOS_CHACHA20_KEY_SIZE) {
      "ChaCha20-Poly1305 key must be exactly $IOS_CHACHA20_KEY_SIZE bytes."
    }
    require(nonce.size == IOS_CHACHA20_NONCE_SIZE) {
      "ChaCha20-Poly1305 nonce must be exactly $IOS_CHACHA20_NONCE_SIZE bytes."
    }
    return requireNotNull(
        currentDelegate()
          .chaCha20Poly1305Decrypt(
            key = key.toNSData(),
            nonce = nonce.toNSData(),
            aad = aad.toNSData(),
            ciphertext = ciphertext.toNSData(),
          )
      ) {
        "IosCryptoProvider delegate failed to decrypt a ChaCha20-Poly1305 payload."
      }
      .toByteArray()
  }

  override fun hkdfSha256(
    ikm: ByteArray,
    salt: ByteArray,
    info: ByteArray,
    outputLength: Int,
  ): ByteArray {
    require(outputLength >= 0) { "HKDF outputLength must be non-negative." }
    return CryptoOutputValidator.requireExactSize(
      bytes =
        requireNotNull(
            currentDelegate()
              .hkdfSha256(
                ikm = ikm.toNSData(),
                salt = salt.toNSData(),
                info = info.toNSData(),
                outputLength = outputLength,
              )
          ) {
            "IosCryptoProvider delegate failed to derive HKDF-SHA256 output."
          }
          .toByteArray(),
      expectedSize = outputLength,
      label = "HKDF-SHA256 output",
    )
  }

  override fun hmacSha256(key: ByteArray, message: ByteArray): ByteArray {
    return CryptoOutputValidator.requireExactSize(
      bytes =
        requireNotNull(
            currentDelegate().hmacSha256(key = key.toNSData(), message = message.toNSData())
          ) {
            "IosCryptoProvider delegate failed to compute an HMAC-SHA256 digest."
          }
          .toByteArray(),
      expectedSize = HMAC_SHA256_SIZE,
      label = "HMAC-SHA256 digest",
    )
  }

  private fun currentDelegate(): IosCryptoDelegate {
    return requireIosCryptoDelegate()
  }
}
