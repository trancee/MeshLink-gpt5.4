package ch.trancee.meshlink.engine

import ch.trancee.meshlink.api.MeshLinkState
import ch.trancee.meshlink.api.PeerIdHex
import ch.trancee.meshlink.crypto.noise.HandshakeRole
import ch.trancee.meshlink.transport.VirtualMeshTransport
import ch.trancee.meshlink.wire.messages.HandshakeMessage
import ch.trancee.meshlink.wire.messages.HandshakeRound
import ch.trancee.meshlink.wire.messages.RoutedMessage
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse

public class MeshEngineIntegrationTest {
  @Test
  public fun harness_deliversPayloadsBetweenTwoRunningEngines(): Unit {
    // Arrange
    val harness = MeshTestHarness.createConnected()
    val payload = byteArrayOf(0x01, 0x02, 0x03)

    // Act
    harness.firstEngine.start()
    harness.secondEngine.start()
    harness.firstEngine.send(peerId = harness.secondPeerId, payload = payload)
    val transportedPayload = harness.secondTransport.receivedFrames.replayCache.single()
    harness.secondEngine.receiveInboundMessage(
      peerId = harness.firstPeerId,
      message = RoutedMessage(hopCount = 1u, maxHops = 4u, payload = transportedPayload),
    )
    val actual = harness.secondEngine.messages.replayCache.single()

    // Assert
    assertEquals(expected = MeshLinkState.RUNNING, actual = harness.firstEngine.state.value)
    assertEquals(expected = MeshLinkState.RUNNING, actual = harness.secondEngine.state.value)
    assertContentEquals(expected = payload, actual = actual)
  }

  @Test
  public fun harness_completesTheThreeMessageNoiseHandshakeAcrossTwoEngines(): Unit {
    // Arrange
    val harness = MeshTestHarness.createConnected()

    // Act
    val first: HandshakeMessage =
      harness.firstEngine.beginHandshake(
        peerId = harness.secondPeerId,
        role = HandshakeRole.INITIATOR,
        payload = byteArrayOf(0x11),
      )
    harness.secondEngine.receiveInboundMessage(
      peerId = harness.firstPeerId,
      message = first,
      handshakeRole = HandshakeRole.RESPONDER,
    )
    val second: HandshakeMessage =
      harness.secondEngine.continueHandshake(
        peerId = harness.firstPeerId,
        payload = byteArrayOf(0x12),
      )
    harness.firstEngine.receiveInboundMessage(
      peerId = harness.secondPeerId,
      message = second,
      handshakeRole = HandshakeRole.INITIATOR,
    )
    val third: HandshakeMessage =
      harness.firstEngine.continueHandshake(
        peerId = harness.secondPeerId,
        payload = byteArrayOf(0x13),
      )
    harness.secondEngine.receiveInboundMessage(
      peerId = harness.firstPeerId,
      message = third,
      handshakeRole = HandshakeRole.RESPONDER,
    )

    // Assert
    assertEquals(expected = HandshakeRound.ONE, actual = first.round)
    assertEquals(expected = HandshakeRound.TWO, actual = second.round)
    assertEquals(expected = HandshakeRound.THREE, actual = third.round)
    assertFalse(
      actual = harness.firstEngine.handshakeManager.isHandshakeActive(peerId = harness.secondPeerId)
    )
    assertFalse(
      actual = harness.secondEngine.handshakeManager.isHandshakeActive(peerId = harness.firstPeerId)
    )
  }
}

private class MeshTestHarness
private constructor(
  val firstPeerId: PeerIdHex,
  val secondPeerId: PeerIdHex,
  val firstTransport: VirtualMeshTransport,
  val secondTransport: VirtualMeshTransport,
  val firstEngine: MeshEngine,
  val secondEngine: MeshEngine,
) {
  companion object {
    fun createConnected(): MeshTestHarness {
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
            cryptoProvider = FakeCryptoProvider(),
          ),
        secondEngine =
          MeshEngine.create(
            config = MeshEngineConfig.default(),
            transport = secondTransport,
            cryptoProvider = FakeCryptoProvider(),
          ),
      )
    }
  }
}
