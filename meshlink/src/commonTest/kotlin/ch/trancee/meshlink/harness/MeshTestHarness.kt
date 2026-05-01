package ch.trancee.meshlink.harness

import ch.trancee.meshlink.api.PeerIdHex
import ch.trancee.meshlink.crypto.CryptoProvider
import ch.trancee.meshlink.crypto.KeyPair
import ch.trancee.meshlink.engine.MeshEngine
import ch.trancee.meshlink.engine.MeshEngineConfig
import ch.trancee.meshlink.transport.VirtualMeshTransport

internal class MeshTestHarness
private constructor(
  internal val firstPeerId: PeerIdHex,
  internal val secondPeerId: PeerIdHex,
  internal val firstTransport: VirtualMeshTransport,
  internal val secondTransport: VirtualMeshTransport,
  internal val firstEngine: MeshEngine,
  internal val secondEngine: MeshEngine,
) {
  internal companion object {
    internal fun createConnected(): MeshTestHarness {
      val firstPeerId = PeerIdHex(value = "00112233")
      val secondPeerId = PeerIdHex(value = "44556677")
      val firstTransport = VirtualMeshTransport(localPeerId = firstPeerId)
      val secondTransport = VirtualMeshTransport(localPeerId = secondPeerId)
      firstTransport.attachPeer(peerId = secondPeerId, transport = secondTransport)
      secondTransport.attachPeer(peerId = firstPeerId, transport = firstTransport)
      firstTransport.connect(peerId = secondPeerId)
      secondTransport.connect(peerId = firstPeerId)

      return MeshTestHarness(
        firstPeerId = firstPeerId,
        secondPeerId = secondPeerId,
        firstTransport = firstTransport,
        secondTransport = secondTransport,
        firstEngine =
          MeshEngine.create(
            config = MeshEngineConfig.default(),
            transport = firstTransport,
            cryptoProvider = MeshHarnessCryptoProvider(),
          ),
        secondEngine =
          MeshEngine.create(
            config = MeshEngineConfig.default(),
            transport = secondTransport,
            cryptoProvider = MeshHarnessCryptoProvider(),
          ),
      )
    }
  }
}

private class MeshHarnessCryptoProvider : CryptoProvider {
  override fun generateX25519KeyPair(): KeyPair = error("Unused in test")

  override fun generateEd25519KeyPair(): KeyPair = error("Unused in test")

  override fun x25519(privateKey: ByteArray, publicKey: ByteArray): ByteArray =
    error("Unused in test")

  override fun ed25519Sign(privateKey: ByteArray, message: ByteArray): ByteArray =
    error("Unused in test")

  override fun ed25519Verify(
    publicKey: ByteArray,
    message: ByteArray,
    signature: ByteArray,
  ): Boolean = error("Unused in test")

  override fun chaCha20Poly1305Encrypt(
    key: ByteArray,
    nonce: ByteArray,
    aad: ByteArray,
    plaintext: ByteArray,
  ): ByteArray = error("Unused in test")

  override fun chaCha20Poly1305Decrypt(
    key: ByteArray,
    nonce: ByteArray,
    aad: ByteArray,
    ciphertext: ByteArray,
  ): ByteArray = error("Unused in test")

  override fun hkdfSha256(
    ikm: ByteArray,
    salt: ByteArray,
    info: ByteArray,
    outputLength: Int,
  ): ByteArray = error("Unused in test")

  override fun hmacSha256(key: ByteArray, message: ByteArray): ByteArray {
    return ByteArray(size = 32) { index ->
      val keyByte: Int = key[index % key.size].toInt() and 0xFF
      val messageByte: Int = message[index % message.size].toInt() and 0xFF
      ((keyByte + messageByte + index) and 0xFF).toByte()
    }
  }
}
