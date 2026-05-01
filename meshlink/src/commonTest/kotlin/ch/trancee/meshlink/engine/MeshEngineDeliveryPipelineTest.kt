package ch.trancee.meshlink.engine

import ch.trancee.meshlink.api.PeerIdHex
import ch.trancee.meshlink.transport.VirtualMeshTransport
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

public class MeshEngineDeliveryPipelineTest {
  @Test
  public fun send_routesOutboundPayloadsThroughTheDeliveryPipelineBeforeUsingTheTransport(): Unit {
    // Arrange
    val localPeerId = PeerIdHex(value = "00112233")
    val remotePeerId = PeerIdHex(value = "44556677")
    val transport = VirtualMeshTransport(localPeerId = localPeerId)
    val remoteTransport = VirtualMeshTransport(localPeerId = remotePeerId)
    transport.attachPeer(peerId = remotePeerId, transport = remoteTransport)
    transport.connect(peerId = remotePeerId)
    val engine =
      MeshEngine.create(
        config = MeshEngineConfig.default(),
        transport = transport,
        cryptoProvider = FakeCryptoProvider(),
      )
    val payload = byteArrayOf(0x01, 0x02, 0x03)

    // Act
    engine.send(peerId = remotePeerId, payload = payload)
    val transportedPayload = remoteTransport.receivedFrames.replayCache.single()
    val pendingDeliveries = engine.deliveryPipeline.pendingCount()

    // Assert
    assertContentEquals(expected = payload, actual = transportedPayload)
    assertEquals(expected = 1, actual = pendingDeliveries)
  }

  @Test
  public fun send_doesNotForwardPayloadsWhenTheDeliveryPipelineQueuesTheRequest(): Unit {
    // Arrange
    val localPeerId = PeerIdHex(value = "00112233")
    val remotePeerId = PeerIdHex(value = "44556677")
    val transport = VirtualMeshTransport(localPeerId = localPeerId)
    val remoteTransport = VirtualMeshTransport(localPeerId = remotePeerId)
    transport.attachPeer(peerId = remotePeerId, transport = remoteTransport)
    transport.connect(peerId = remotePeerId)
    val engine =
      MeshEngine.create(
        config = MeshEngineConfig.default(),
        transport = transport,
        cryptoProvider = FakeCryptoProvider(),
      )

    // Act
    repeat(times = 33) { index ->
      engine.send(peerId = remotePeerId, payload = byteArrayOf(index.toByte()))
    }
    val actualPendingDeliveries = engine.deliveryPipeline.pendingCount()

    // Assert
    assertEquals(expected = 32, actual = actualPendingDeliveries)
  }
}
