package ch.trancee.meshlink.engine

import ch.trancee.meshlink.api.DiagnosticCode
import ch.trancee.meshlink.api.DiagnosticPayload
import ch.trancee.meshlink.api.DiagnosticSink
import ch.trancee.meshlink.api.PeerDetail
import ch.trancee.meshlink.api.PeerIdHex
import ch.trancee.meshlink.api.PeerState
import ch.trancee.meshlink.transport.VirtualMeshTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

public class PeerLifecycleIntegrationTest {
    @Test
    public fun publishPeers_updatesThePeerSnapshotAndEmitsPeerLifecycleDiagnostics(): Unit {
        // Arrange
        val diagnosticSink = DiagnosticSink.create(bufferSize = 8, clock = { 88L })
        val peerId = PeerIdHex(value = "44556677")
        val engine = MeshEngine.create(
            config = MeshEngineConfig.default(),
            transport = VirtualMeshTransport(localPeerId = PeerIdHex(value = "00112233")),
            diagnosticSink = diagnosticSink,
            cryptoProvider = FakeCryptoProvider(),
        )
        val discoveredPeer = PeerDetail(
            peerId = peerId,
            state = PeerState.Discovered,
            displayName = "Alice",
            lastSeenEpochMillis = 1L,
        )
        val connectedPeer = discoveredPeer.copy(state = PeerState.Connected, lastSeenEpochMillis = 2L)

        // Act
        engine.publishPeers(peerDetails = listOf(discoveredPeer))
        engine.publishPeers(peerDetails = listOf(connectedPeer))
        val actualPeers = engine.peers.value
        val actualStates = diagnosticSink.diagnosticEvents.replayCache.map { event ->
            assertEquals(expected = DiagnosticCode.PEER_DISCOVERED, actual = event.code)
            val payload = assertIs<DiagnosticPayload.PeerLifecycle>(event.payload)
            payload.state
        }

        // Assert
        assertEquals(expected = listOf(connectedPeer), actual = actualPeers)
        assertEquals(expected = listOf(PeerState.Discovered, PeerState.Connected), actual = actualStates)
    }

    @Test
    public fun publishPeers_preservesDisconnectedPeerStates(): Unit {
        // Arrange
        val diagnosticSink = DiagnosticSink.create(bufferSize = 4, clock = { 99L })
        val peer = PeerDetail(
            peerId = PeerIdHex(value = "8899aabb"),
            state = PeerState.Disconnected,
            displayName = null,
            lastSeenEpochMillis = 3L,
        )
        val engine = MeshEngine.create(
            config = MeshEngineConfig.default(),
            transport = VirtualMeshTransport(localPeerId = PeerIdHex(value = "00112233")),
            diagnosticSink = diagnosticSink,
            cryptoProvider = FakeCryptoProvider(),
        )

        // Act
        engine.publishPeers(peerDetails = listOf(peer))
        val payload = assertIs<DiagnosticPayload.PeerLifecycle>(diagnosticSink.diagnosticEvents.replayCache.single().payload)

        // Assert
        assertEquals(expected = PeerState.Disconnected, actual = payload.state)
        assertEquals(expected = listOf(peer), actual = engine.peers.value)
    }
}
