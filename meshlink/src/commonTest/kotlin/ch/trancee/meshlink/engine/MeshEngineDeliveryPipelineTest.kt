package ch.trancee.meshlink.engine

import ch.trancee.meshlink.api.PeerIdHex
import ch.trancee.meshlink.messaging.SendResult
import ch.trancee.meshlink.routing.RoutingUpdate
import ch.trancee.meshlink.transport.VirtualMeshTransport
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

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
  public fun sendRouted_buffersPayloadsUntilARouteBecomesAvailable(): Unit {
    // Arrange
    val localPeerId = PeerIdHex(value = "00112233")
    val recipientPeerId = PeerIdHex(value = "44556677")
    val nextHopPeerId = PeerIdHex(value = "8899aabb")
    val transport = VirtualMeshTransport(localPeerId = localPeerId)
    val nextHopTransport = VirtualMeshTransport(localPeerId = nextHopPeerId)
    transport.attachPeer(peerId = nextHopPeerId, transport = nextHopTransport)
    transport.connect(peerId = nextHopPeerId)
    val engine =
      MeshEngine.create(
        config = MeshEngineConfig.default(),
        transport = transport,
        cryptoProvider = FakeCryptoProvider(),
      )
    val payload = byteArrayOf(0x09, 0x08)

    // Act
    val queued = engine.sendRouted(peerId = recipientPeerId, payload = payload, nowEpochMillis = 0L)
    engine.processRoutingUpdate(
      update =
        RoutingUpdate(
          destinationPeerId = recipientPeerId,
          nextHopPeerId = nextHopPeerId,
          metric = 1,
          sequenceNumber = 3,
          expiresAtEpochMillis = 1_000L,
        ),
      nowEpochMillis = 1L,
    )
    val actual = nextHopTransport.receivedFrames.replayCache.single()

    // Assert
    assertIs<SendResult.Queued>(queued)
    assertContentEquals(expected = payload, actual = actual)
    assertEquals(expected = 1, actual = engine.deliveryPipeline.pendingCount())
    assertEquals(expected = 0, actual = engine.deliveryPipeline.bufferedCount())
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
