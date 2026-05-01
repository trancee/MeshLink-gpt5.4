package ch.trancee.meshlink.transport

import ch.trancee.meshlink.api.DiagnosticCode
import ch.trancee.meshlink.api.DiagnosticPayload
import ch.trancee.meshlink.api.DiagnosticSink
import ch.trancee.meshlink.api.PeerIdHex
import ch.trancee.meshlink.api.PeerState
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

public class VirtualMeshTransportTest {
  @Test
  public fun advertise_updatesTheAdvertisingState(): Unit {
    // Arrange
    val transport = VirtualMeshTransport(localPeerId = PeerIdHex(value = "00112233"))

    // Act
    transport.advertise(enabled = true)
    val enabledState = transport.isAdvertising.value
    transport.advertise(enabled = false)
    val disabledState = transport.isAdvertising.value

    // Assert
    assertTrue(actual = enabledState)
    assertFalse(actual = disabledState)
  }

  @Test
  public fun connect_marksAttachedPeersAsConnected(): Unit {
    // Arrange
    val localPeerId = PeerIdHex(value = "00112233")
    val remotePeerId = PeerIdHex(value = "44556677")
    val transport = VirtualMeshTransport(localPeerId = localPeerId)
    transport.attachPeer(
      peerId = remotePeerId,
      transport = VirtualMeshTransport(localPeerId = remotePeerId),
    )

    // Act
    transport.connect(peerId = remotePeerId)

    // Assert
    assertTrue(actual = transport.isConnected(peerId = remotePeerId))
  }

  @Test
  public fun connect_ignoresPeersThatHaveNotBeenAttached(): Unit {
    // Arrange
    val remotePeerId = PeerIdHex(value = "44556677")
    val transport = VirtualMeshTransport(localPeerId = PeerIdHex(value = "00112233"))

    // Act
    transport.connect(peerId = remotePeerId)

    // Assert
    assertFalse(actual = transport.isConnected(peerId = remotePeerId))
  }

  @Test
  public fun disconnect_removesConnectedPeers(): Unit {
    // Arrange
    val remotePeerId = PeerIdHex(value = "44556677")
    val transport = VirtualMeshTransport(localPeerId = PeerIdHex(value = "00112233"))
    transport.attachPeer(
      peerId = remotePeerId,
      transport = VirtualMeshTransport(localPeerId = remotePeerId),
    )
    transport.connect(peerId = remotePeerId)

    // Act
    transport.disconnect(peerId = remotePeerId)

    // Assert
    assertFalse(actual = transport.isConnected(peerId = remotePeerId))
  }

  @Test
  public fun disconnect_ignoresPeersThatWereNeverConnected(): Unit {
    // Arrange
    val remotePeerId = PeerIdHex(value = "44556677")
    val transport = VirtualMeshTransport(localPeerId = PeerIdHex(value = "00112233"))

    // Act
    transport.disconnect(peerId = remotePeerId)

    // Assert
    assertFalse(actual = transport.isConnected(peerId = remotePeerId))
  }

  @Test
  public fun send_deliversFramesToConnectedAttachedPeers(): Unit {
    // Arrange
    val localPeerId = PeerIdHex(value = "00112233")
    val remotePeerId = PeerIdHex(value = "44556677")
    val localTransport = VirtualMeshTransport(localPeerId = localPeerId)
    val remoteTransport = VirtualMeshTransport(localPeerId = remotePeerId)
    localTransport.attachPeer(peerId = remotePeerId, transport = remoteTransport)
    localTransport.connect(peerId = remotePeerId)
    val payload = byteArrayOf(0x01, 0x02, 0x03)

    // Act
    localTransport.send(peerId = remotePeerId, payload = payload)
    val actual = remoteTransport.receivedFrames.replayCache.single()

    // Assert
    assertContentEquals(expected = payload, actual = actual)
  }

  @Test
  public fun send_ignoresDetachedPeers(): Unit {
    // Arrange
    val remotePeerId = PeerIdHex(value = "44556677")
    val transport = VirtualMeshTransport(localPeerId = PeerIdHex(value = "00112233"))

    // Act
    transport.send(peerId = remotePeerId, payload = byteArrayOf(0x01))

    // Assert
    assertTrue(actual = transport.receivedFrames.replayCache.isEmpty())
  }

  @Test
  public fun send_ignoresPeersThatWereNotConnected(): Unit {
    // Arrange
    val localPeerId = PeerIdHex(value = "00112233")
    val remotePeerId = PeerIdHex(value = "44556677")
    val localTransport = VirtualMeshTransport(localPeerId = localPeerId)
    val remoteTransport = VirtualMeshTransport(localPeerId = remotePeerId)
    localTransport.attachPeer(peerId = remotePeerId, transport = remoteTransport)

    // Act
    localTransport.send(peerId = remotePeerId, payload = byteArrayOf(0x01))

    // Assert
    assertTrue(actual = remoteTransport.receivedFrames.replayCache.isEmpty())
  }

  @Test
  public fun transport_emitsPeerAndMessageDiagnosticsForLifecycleEvents(): Unit {
    // Arrange
    val localDiagnosticSink = DiagnosticSink.create(bufferSize = 8, clock = { 123L })
    val remoteDiagnosticSink = DiagnosticSink.create(bufferSize = 8, clock = { 123L })
    val localPeerId = PeerIdHex(value = "00112233")
    val remotePeerId = PeerIdHex(value = "44556677")
    val localTransport =
      VirtualMeshTransport(localPeerId = localPeerId, diagnosticSink = localDiagnosticSink)
    val remoteTransport =
      VirtualMeshTransport(localPeerId = remotePeerId, diagnosticSink = remoteDiagnosticSink)
    localTransport.attachPeer(peerId = remotePeerId, transport = remoteTransport)

    // Act
    localTransport.connect(peerId = remotePeerId)
    localTransport.send(peerId = remotePeerId, payload = byteArrayOf(0x01))
    localTransport.disconnect(peerId = remotePeerId)
    val localCodes = localDiagnosticSink.diagnosticEvents.replayCache.map { event -> event.code }
    val localFirstPayload =
      localDiagnosticSink.diagnosticEvents.replayCache.first().payload
        as DiagnosticPayload.PeerLifecycle
    val localLastPayload =
      localDiagnosticSink.diagnosticEvents.replayCache.last().payload
        as DiagnosticPayload.PeerLifecycle
    val remoteEvent = remoteDiagnosticSink.diagnosticEvents.replayCache.single()
    val remotePayload = remoteEvent.payload as DiagnosticPayload.PeerLifecycle

    // Assert
    assertEquals(
      expected =
        listOf(
          DiagnosticCode.PEER_DISCOVERED,
          DiagnosticCode.MESSAGE_SENT,
          DiagnosticCode.PEER_LOST,
        ),
      actual = localCodes,
    )
    assertEquals(expected = remotePeerId, actual = localFirstPayload.peerId)
    assertEquals(expected = PeerState.Connected, actual = localFirstPayload.state)
    assertEquals(expected = remotePeerId, actual = localLastPayload.peerId)
    assertEquals(expected = PeerState.Disconnected, actual = localLastPayload.state)
    assertEquals(expected = DiagnosticCode.MESSAGE_DELIVERED, actual = remoteEvent.code)
    assertEquals(expected = localPeerId, actual = remotePayload.peerId)
    assertEquals(expected = PeerState.Connected, actual = remotePayload.state)
  }
}
