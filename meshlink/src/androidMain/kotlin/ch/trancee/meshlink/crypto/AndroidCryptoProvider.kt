package ch.trancee.meshlink.crypto

import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.interfaces.EdECPrivateKey
import java.security.interfaces.EdECPublicKey
import java.security.interfaces.XECPrivateKey
import java.security.interfaces.XECPublicKey
import java.security.spec.EdECPoint
import java.security.spec.EdECPrivateKeySpec
import java.security.spec.EdECPublicKeySpec
import java.security.spec.NamedParameterSpec
import java.security.spec.XECPrivateKeySpec
import java.security.spec.XECPublicKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

public class AndroidCryptoProvider : CryptoProvider {
  override fun generateX25519KeyPair(): KeyPair {
    val generator = KeyPairGenerator.getInstance("X25519")
    val keyPair = generator.generateKeyPair()
    return KeyPair(
      publicKey =
        toLittleEndian(value = (keyPair.public as XECPublicKey).u, size = X25519_KEY_SIZE),
      secretKey = (keyPair.private as XECPrivateKey).scalar.get(),
    )
  }

  override fun generateEd25519KeyPair(): KeyPair {
    val generator = KeyPairGenerator.getInstance("Ed25519")
    val keyPair = generator.generateKeyPair()
    val publicKey: ByteArray = encodeEd25519PublicKey(key = keyPair.public as EdECPublicKey)
    val privateSeed: ByteArray = (keyPair.private as EdECPrivateKey).bytes.get()
    return KeyPair(publicKey = publicKey, secretKey = privateSeed + publicKey)
  }

  override fun x25519(privateKey: ByteArray, publicKey: ByteArray): ByteArray {
    require(privateKey.size == X25519_KEY_SIZE) {
      "X25519 privateKey must be exactly $X25519_KEY_SIZE bytes."
    }
    require(publicKey.size == X25519_KEY_SIZE) {
      "X25519 publicKey must be exactly $X25519_KEY_SIZE bytes."
    }
    val keyFactory = KeyFactory.getInstance("X25519")
    val privateKeySpec = XECPrivateKeySpec(NamedParameterSpec.X25519, privateKey)
    val publicKeySpec =
      XECPublicKeySpec(NamedParameterSpec.X25519, fromLittleEndian(bytes = publicKey))
    val agreement = KeyAgreement.getInstance("X25519")
    agreement.init(keyFactory.generatePrivate(privateKeySpec))
    agreement.doPhase(keyFactory.generatePublic(publicKeySpec), true)
    return agreement.generateSecret()
  }

  override fun ed25519Sign(privateKey: ByteArray, message: ByteArray): ByteArray {
    require(privateKey.size == Identity.SECRET_KEY_SIZE) {
      "Ed25519 secretKey must be exactly ${Identity.SECRET_KEY_SIZE} bytes."
    }
    val privateSeed: ByteArray =
      privateKey.copyOfRange(fromIndex = 0, toIndex = Identity.PUBLIC_KEY_SIZE)
    val keyFactory = KeyFactory.getInstance("Ed25519")
    val privateKeySpec = EdECPrivateKeySpec(NamedParameterSpec.ED25519, privateSeed)
    val signature = Signature.getInstance("Ed25519")
    signature.initSign(keyFactory.generatePrivate(privateKeySpec))
    signature.update(message)
    return signature.sign()
  }

  override fun ed25519Verify(
    publicKey: ByteArray,
    message: ByteArray,
    signature: ByteArray,
  ): Boolean {
    require(publicKey.size == Identity.PUBLIC_KEY_SIZE) {
      "Ed25519 publicKey must be exactly ${Identity.PUBLIC_KEY_SIZE} bytes."
    }
    val point = decodeEd25519Point(publicKey = publicKey)
    val keyFactory = KeyFactory.getInstance("Ed25519")
    val publicKeySpec = EdECPublicKeySpec(NamedParameterSpec.ED25519, point)
    val verifier = Signature.getInstance("Ed25519")
    verifier.initVerify(keyFactory.generatePublic(publicKeySpec))
    verifier.update(message)
    return verifier.verify(signature)
  }

  override fun chaCha20Poly1305Encrypt(
    key: ByteArray,
    nonce: ByteArray,
    aad: ByteArray,
    plaintext: ByteArray,
  ): ByteArray {
    require(key.size == CHACHA20_KEY_SIZE) {
      "ChaCha20-Poly1305 key must be exactly $CHACHA20_KEY_SIZE bytes."
    }
    require(nonce.size == CHACHA20_NONCE_SIZE) {
      "ChaCha20-Poly1305 nonce must be exactly $CHACHA20_NONCE_SIZE bytes."
    }
    val cipher = Cipher.getInstance("ChaCha20-Poly1305")
    cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "ChaCha20"), IvParameterSpec(nonce))
    if (aad.isNotEmpty()) {
      cipher.updateAAD(aad)
    }
    return cipher.doFinal(plaintext)
  }

  override fun chaCha20Poly1305Decrypt(
    key: ByteArray,
    nonce: ByteArray,
    aad: ByteArray,
    ciphertext: ByteArray,
  ): ByteArray {
    require(key.size == CHACHA20_KEY_SIZE) {
      "ChaCha20-Poly1305 key must be exactly $CHACHA20_KEY_SIZE bytes."
    }
    require(nonce.size == CHACHA20_NONCE_SIZE) {
      "ChaCha20-Poly1305 nonce must be exactly $CHACHA20_NONCE_SIZE bytes."
    }
    val cipher = Cipher.getInstance("ChaCha20-Poly1305")
    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "ChaCha20"), IvParameterSpec(nonce))
    if (aad.isNotEmpty()) {
      cipher.updateAAD(aad)
    }
    return cipher.doFinal(ciphertext)
  }

  override fun hkdfSha256(
    ikm: ByteArray,
    salt: ByteArray,
    info: ByteArray,
    outputLength: Int,
  ): ByteArray {
    require(outputLength >= 0) { "HKDF outputLength must be non-negative." }
    val pseudoRandomKey: ByteArray =
      hmacSha256(
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
      previousBlock.copyInto(
        destination = output,
        destinationOffset = generatedBytes,
        endIndex = bytesToCopy,
      )
      generatedBytes += bytesToCopy
      counter += 1
    }

    return output
  }

  override fun hmacSha256(key: ByteArray, message: ByteArray): ByteArray {
    val mac: Mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(key, "HmacSHA256"))
    return mac.doFinal(message)
  }

  private fun encodeEd25519PublicKey(key: EdECPublicKey): ByteArray {
    val point: EdECPoint = key.point
    val encoded: ByteArray = toLittleEndian(value = point.y, size = Identity.PUBLIC_KEY_SIZE)
    encoded[encoded.lastIndex] =
      (encoded.last().toInt() or (point.isXOdd.compareTo(false) shl 7)).toByte()
    return encoded
  }

  private fun decodeEd25519Point(publicKey: ByteArray): EdECPoint {
    val yBytes: ByteArray =
      publicKey.copyOf().also { bytes ->
        bytes[bytes.lastIndex] = (bytes.last().toInt() and 0x7F).toByte()
      }
    return EdECPoint((publicKey.last().toInt() and 0x80) != 0, fromLittleEndian(bytes = yBytes))
  }

  private fun toLittleEndian(value: BigInteger, size: Int): ByteArray {
    var remaining: BigInteger = value
    return ByteArray(size = size).also { output ->
      for (index in 0 until size) {
        output[index] = remaining.and(BIG_INTEGER_255).toByte()
        remaining = remaining.shiftRight(8)
      }
    }
  }

  private fun fromLittleEndian(bytes: ByteArray): BigInteger {
    val bigEndian: ByteArray = ByteArray(size = bytes.size + 1)
    for (index in bytes.indices) {
      bigEndian[bigEndian.lastIndex - index] = bytes[index]
    }
    return BigInteger(bigEndian)
  }

  public companion object {
    private const val HASH_OUTPUT_SIZE: Int = 32
    private const val CHACHA20_KEY_SIZE: Int = 32
    private const val CHACHA20_NONCE_SIZE: Int = 12
    private const val X25519_KEY_SIZE: Int = 32
    private val BIG_INTEGER_255: BigInteger = BigInteger.valueOf(0xFF)
  }
}
