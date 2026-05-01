package ch.trancee.meshlink.engine

import ch.trancee.meshlink.api.DiagnosticCode
import ch.trancee.meshlink.api.DiagnosticSink
import ch.trancee.meshlink.api.MeshLinkState
import ch.trancee.meshlink.api.PeerIdHex
import ch.trancee.meshlink.crypto.noise.HandshakeRole
import ch.trancee.meshlink.transport.VirtualMeshTransport
import ch.trancee.meshlink.wire.messages.BroadcastMessage
import ch.trancee.meshlink.wire.messages.HelloMessage
import ch.trancee.meshlink.wire.messages.RoutedMessage
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

public class MeshEngineTest {
  @Test
  public fun scenario01_create_wiresTheConfiguredTransportAndStartsUninitialized(): Unit {
    // Arrange
    val config = MeshEngineConfig.default()
    val transport = VirtualMeshTransport(localPeerId = PeerIdHex(value = "00112233"))

    // Act
    val engine =
      MeshEngine.create(
        config = config,
        transport = transport,
        cryptoProvider = FakeCryptoProvider(),
      )

    // Assert
    assertSame(expected = config, actual = engine.config)
    assertSame(expected = transport, actual = engine.transport)
    assertEquals(expected = MeshLinkState.UNINITIALIZED, actual = engine.state.value)
  }

  @Test
  public fun scenario02_start_transitionsToRunningAndEnablesAdvertising(): Unit {
    // Arrange
    val transport = VirtualMeshTransport(localPeerId = PeerIdHex(value = "00112233"))
    val engine =
      MeshEngine.create(
        config = MeshEngineConfig.default(),
        transport = transport,
        cryptoProvider = FakeCryptoProvider(),
      )

    // Act
    engine.start()

    // Assert
    assertEquals(expected = MeshLinkState.RUNNING, actual = engine.state.value)
    assertTrue(actual = transport.isAdvertising.value)
  }

  @Test
  public fun scenario03_pauseAndResume_toggleAdvertisingWithoutLosingRunningCapability(): Unit {
    // Arrange
    val transport = VirtualMeshTransport(localPeerId = PeerIdHex(value = "00112233"))
    val engine =
      MeshEngine.create(
        config = MeshEngineConfig.default(),
        transport = transport,
        cryptoProvider = FakeCryptoProvider(),
      )
    engine.start()

    // Act
    engine.pause()
    val pausedState = engine.state.value
    val pausedAdvertising = transport.isAdvertising.value
    engine.resume()
    val resumedState = engine.state.value
    val resumedAdvertising = transport.isAdvertising.value

    // Assert
    assertEquals(expected = MeshLinkState.PAUSED, actual = pausedState)
    assertFalse(actual = pausedAdvertising)
    assertEquals(expected = MeshLinkState.RUNNING, actual = resumedState)
    assertTrue(actual = resumedAdvertising)
  }

  @Test
  public fun scenario04_stop_transitionsToStoppedAndDisablesAdvertising(): Unit {
    // Arrange
    val transport = VirtualMeshTransport(localPeerId = PeerIdHex(value = "00112233"))
    val engine =
      MeshEngine.create(
        config = MeshEngineConfig.default(),
        transport = transport,
        cryptoProvider = FakeCryptoProvider(),
      )
    engine.start()

    // Act
    engine.stop()

    // Assert
    assertEquals(expected = MeshLinkState.STOPPED, actual = engine.state.value)
    assertFalse(actual = transport.isAdvertising.value)
  }

  @Test
  public fun scenario05_send_forwardsPayloadsToTheTransport(): Unit {
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
    val actual = remoteTransport.receivedFrames.replayCache.single()

    // Assert
    assertContentEquals(expected = payload, actual = actual)
  }

  @Test
  public fun scenario06_receiveInboundMessage_routesRoutedPayloadsToMessages(): Unit {
    // Arrange
    val engine =
      MeshEngine.create(
        config = MeshEngineConfig.default(),
        transport = VirtualMeshTransport(localPeerId = PeerIdHex(value = "00112233")),
        cryptoProvider = FakeCryptoProvider(),
      )
    val payload = byteArrayOf(0x0A, 0x0B)

    // Act
    engine.receiveInboundMessage(
      peerId = PeerIdHex(value = "44556677"),
      message = RoutedMessage(hopCount = 1u, maxHops = 4u, payload = payload),
    )
    val actual = engine.messages.replayCache.single()

    // Assert
    assertContentEquals(expected = payload, actual = actual)
  }

  @Test
  public fun scenario07_receiveInboundMessage_routesBroadcastPayloadsToMessages(): Unit {
    // Arrange
    val engine =
      MeshEngine.create(
        config = MeshEngineConfig.default(),
        transport = VirtualMeshTransport(localPeerId = PeerIdHex(value = "00112233")),
        cryptoProvider = FakeCryptoProvider(),
      )
    val payload = byteArrayOf(0x0C, 0x0D)

    // Act
    engine.receiveInboundMessage(
      peerId = PeerIdHex(value = "44556677"),
      message =
        BroadcastMessage(
          originPeerId = byteArrayOf(0x01),
          sequenceNumber = 9,
          maxHops = 2u,
          payload = payload,
        ),
    )
    val actual = engine.messages.replayCache.single()

    // Assert
    assertContentEquals(expected = payload, actual = actual)
  }

  @Test
  public fun scenario08_receiveInboundMessage_routesHandshakeFramesToTheHandshakeManager(): Unit {
    // Arrange
    val diagnosticSink = DiagnosticSink.create(bufferSize = 4, clock = { 77L })
    val peerId = PeerIdHex(value = "44556677")
    val engine =
      MeshEngine.create(
        config = MeshEngineConfig.default(),
        transport = VirtualMeshTransport(localPeerId = PeerIdHex(value = "00112233")),
        diagnosticSink = diagnosticSink,
        cryptoProvider = FakeCryptoProvider(),
      )

    // Act
    val firstHandshakeFrame =
      ch.trancee.meshlink.crypto.noise
        .NoiseXXHandshake(role = HandshakeRole.INITIATOR)
        .createOutboundMessage(payload = byteArrayOf(0x0E))
    engine.receiveInboundMessage(
      peerId = peerId,
      message = firstHandshakeFrame,
      handshakeRole = HandshakeRole.RESPONDER,
    )

    // Assert
    assertTrue(actual = engine.handshakeManager.isHandshakeActive(peerId = peerId))
    assertEquals(
      expected = listOf(DiagnosticCode.HANDSHAKE_STARTED),
      actual = diagnosticSink.diagnosticEvents.replayCache.map { event -> event.code },
    )
  }

  @Test
  public fun scenario09_receiveInboundMessage_ignoresNonHandshakeControlMessages(): Unit {
    // Arrange
    val engine =
      MeshEngine.create(
        config = MeshEngineConfig.default(),
        transport = VirtualMeshTransport(localPeerId = PeerIdHex(value = "00112233")),
        cryptoProvider = FakeCryptoProvider(),
      )
    val peerId = PeerIdHex(value = "44556677")

    // Act
    engine.receiveInboundMessage(
      peerId = peerId,
      message = HelloMessage(peerId = byteArrayOf(0x01), appIdHash = 42),
    )

    // Assert
    assertTrue(actual = engine.messages.replayCache.isEmpty())
    assertFalse(actual = engine.handshakeManager.isHandshakeActive(peerId = peerId))
  }
}
