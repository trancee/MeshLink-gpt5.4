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

    internal fun createLinearNetwork(size: Int): MeshLinearNetworkHarness {
      require(size >= 2) { "MeshTestHarness size must be at least 2." }

      val peerIds: List<PeerIdHex> =
        List(size = size) { index ->
          PeerIdHex(value = (index + 1).toString(radix = 16).padStart(length = 8, padChar = '0'))
        }
      val transports: List<VirtualMeshTransport> =
        peerIds.map { peerId -> VirtualMeshTransport(localPeerId = peerId) }

      for (index in 0 until transports.lastIndex) {
        val localPeerId = peerIds[index]
        val remotePeerId = peerIds[index + 1]
        val localTransport = transports[index]
        val remoteTransport = transports[index + 1]
        localTransport.attachPeer(peerId = remotePeerId, transport = remoteTransport)
        remoteTransport.attachPeer(peerId = localPeerId, transport = localTransport)
        localTransport.connect(peerId = remotePeerId)
        remoteTransport.connect(peerId = localPeerId)
      }

      return MeshLinearNetworkHarness(peerIds = peerIds, transports = transports)
    }
  }
}

internal data class MeshLinearNetworkHarness(
  internal val peerIds: List<PeerIdHex>,
  internal val transports: List<VirtualMeshTransport>,
)

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
