package ch.trancee.meshlink.engine

import ch.trancee.meshlink.api.DiagnosticCode
import ch.trancee.meshlink.api.DiagnosticSink
import ch.trancee.meshlink.api.MeshLinkState
import ch.trancee.meshlink.api.PeerDetail
import ch.trancee.meshlink.api.PeerIdHex
import ch.trancee.meshlink.api.PeerState
import ch.trancee.meshlink.transport.VirtualMeshTransport
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

public class MeshEngineApiLifecycleTest {
  @Test
  public fun create_wiresSubsystemsAndStartsUninitialized(): Unit {
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
    assertTrue(actual = engine.peers.value.isEmpty())
    assertTrue(actual = engine.messages.replayCache.isEmpty())
  }

  @Test
  public fun create_withoutExplicitDependencies_usesThePlatformCryptoProviderPath(): Unit {
    // Arrange
    val config = MeshEngineConfig.default()
    val transport = VirtualMeshTransport(localPeerId = PeerIdHex(value = "00112233"))

    // Act
    val engine = MeshEngine.create(config = config, transport = transport)

    // Assert
    assertEquals(expected = MeshLinkState.UNINITIALIZED, actual = engine.state.value)
    assertSame(expected = transport, actual = engine.transport)
  }

  @Test
  public fun lifecycle_transitionsDriveAdvertisingAndDiagnostics(): Unit {
    // Arrange
    val diagnosticSink = DiagnosticSink.create(bufferSize = 8, clock = { 77L })
    val transport = VirtualMeshTransport(localPeerId = PeerIdHex(value = "00112233"))
    val engine =
      MeshEngine.create(
        config = MeshEngineConfig.default(),
        transport = transport,
        diagnosticSink = diagnosticSink,
        cryptoProvider = FakeCryptoProvider(),
      )

    // Act
    engine.start()
    val runningAdvertising = transport.isAdvertising.value
    engine.pause()
    val pausedAdvertising = transport.isAdvertising.value
    engine.resume()
    val resumedAdvertising = transport.isAdvertising.value
    engine.stop()
    val stoppedAdvertising = transport.isAdvertising.value
    val actualCodes = diagnosticSink.diagnosticEvents.replayCache.map { event -> event.code }

    // Assert
    assertTrue(actual = runningAdvertising)
    assertFalse(actual = pausedAdvertising)
    assertTrue(actual = resumedAdvertising)
    assertFalse(actual = stoppedAdvertising)
    assertEquals(expected = MeshLinkState.STOPPED, actual = engine.state.value)
    assertEquals(
      expected =
        listOf(
          DiagnosticCode.ENGINE_STARTED,
          DiagnosticCode.ENGINE_PAUSED,
          DiagnosticCode.ENGINE_RESUMED,
          DiagnosticCode.ENGINE_STOPPED,
        ),
      actual = actualCodes,
    )
  }

  @Test
  public fun send_delegatesToTheUnderlyingTransport(): Unit {
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
  public fun healthSnapshot_summarizesCurrentRuntimeState(): Unit {
    // Arrange
    val destinationPeerId = PeerIdHex(value = "44556677")
    val engine =
      MeshEngine.create(
        config = MeshEngineConfig.default(),
        transport = VirtualMeshTransport(localPeerId = PeerIdHex(value = "00112233")),
        cryptoProvider = FakeCryptoProvider(),
      )
    engine.publishPeers(
      peerDetails =
        listOf(
          PeerDetail(
            peerId = destinationPeerId,
            state = PeerState.Connected,
            displayName = null,
            lastSeenEpochMillis = 0L,
          )
        )
    )
    engine.processRoutingUpdate(
      update =
        ch.trancee.meshlink.routing.RoutingUpdate(
          destinationPeerId = destinationPeerId,
          nextHopPeerId = destinationPeerId,
          metric = 1,
          sequenceNumber = 4,
          expiresAtEpochMillis = 100L,
        )
    )
    engine.startTransfer(
      transferId = "transfer-1",
      recipientPeerId = destinationPeerId,
      priority = ch.trancee.meshlink.transfer.Priority.NORMAL,
      payload = byteArrayOf(0x01),
      preferL2cap = false,
      nowEpochMillis = 0L,
    )
    engine.sendRouted(
      peerId = PeerIdHex(value = "8899aabb"),
      payload = byteArrayOf(0x02),
      nowEpochMillis = 0L,
    )

    // Act
    val snapshot = engine.healthSnapshot()

    // Assert
    assertEquals(expected = 1, actual = snapshot.connectedPeerCount)
    assertEquals(expected = 1, actual = snapshot.routingTableSize)
    assertEquals(expected = 1, actual = snapshot.activeTransferCount)
    assertEquals(expected = 1, actual = snapshot.bufferedMessageCount)
  }

  @Test
  public fun factoryReset_rejectsCallsBeforeStop(): Unit {
    // Arrange
    val engine =
      MeshEngine.create(
        config = MeshEngineConfig.default(),
        transport = VirtualMeshTransport(localPeerId = PeerIdHex(value = "00112233")),
        cryptoProvider = FakeCryptoProvider(),
      )
    engine.start()

    // Act
    val error = assertFailsWith<IllegalStateException> { engine.factoryReset() }

    // Assert
    assertEquals(
      expected = "MeshEngine must be stopped before factoryReset().",
      actual = error.message,
    )
  }

  @Test
  public fun pause_rejectsInvalidTransitionsBeforeStart(): Unit {
    // Arrange
    val engine =
      MeshEngine.create(
        config = MeshEngineConfig.default(),
        transport = VirtualMeshTransport(localPeerId = PeerIdHex(value = "00112233")),
        cryptoProvider = FakeCryptoProvider(),
      )

    // Act
    val error = assertFailsWith<IllegalStateException> { engine.pause() }

    // Assert
    assertEquals(
      expected = "MeshEngine cannot transition from UNINITIALIZED to PAUSED.",
      actual = error.message,
    )
  }
}
