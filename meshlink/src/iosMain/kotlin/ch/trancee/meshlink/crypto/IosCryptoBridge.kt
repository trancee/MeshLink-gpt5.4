@file:OptIn(kotlinx.cinterop.BetaInteropApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

package ch.trancee.meshlink.crypto

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
public interface IosCryptoDelegate {
  public fun generateX25519KeyPair(): IosCryptoKeyPair?

  public fun generateEd25519KeyPair(): IosCryptoKeyPair?

  public fun x25519(privateKey: NSData, publicKey: NSData): NSData?

  public fun ed25519Sign(privateKey: NSData, message: NSData): NSData?

  public fun ed25519Verify(publicKey: NSData, message: NSData, signature: NSData): Boolean

  public fun chaCha20Poly1305Encrypt(
    key: NSData,
    nonce: NSData,
    aad: NSData,
    plaintext: NSData,
  ): NSData?

  public fun chaCha20Poly1305Decrypt(
    key: NSData,
    nonce: NSData,
    aad: NSData,
    ciphertext: NSData,
  ): NSData?

  public fun hkdfSha256(ikm: NSData, salt: NSData, info: NSData, outputLength: Int): NSData?

  public fun hmacSha256(key: NSData, message: NSData): NSData?
}

@OptIn(ExperimentalForeignApi::class)
public class IosCryptoKeyPair(public val publicKey: NSData, public val secretKey: NSData)

internal object IosCryptoRuntime {
  internal var delegate: IosCryptoDelegate? = null
}

internal fun requireIosCryptoDelegate(): IosCryptoDelegate {
  return requireNotNull(IosCryptoRuntime.delegate) {
    "IosCryptoProvider has not been configured. Install an IosCryptoDelegate before creating MeshLink on iOS."
  }
}

@OptIn(ExperimentalForeignApi::class)
internal fun ByteArray.toNSData(): NSData {
  if (isEmpty()) {
    return NSData.create(bytes = null, length = 0u)
  }

  return usePinned { pinned -> NSData.create(bytes = pinned.addressOf(0), length = size.toULong()) }
}

@OptIn(ExperimentalForeignApi::class)
internal fun NSData.toByteArray(): ByteArray {
  val byteCount: Int = length.toInt()
  if (byteCount == 0) {
    return ByteArray(size = 0)
  }

  return ByteArray(size = byteCount).also { destination ->
    destination.usePinned { pinned -> memcpy(pinned.addressOf(0), bytes, length) }
  }
}
