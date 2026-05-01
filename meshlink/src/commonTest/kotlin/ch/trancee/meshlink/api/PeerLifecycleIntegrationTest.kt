package ch.trancee.meshlink.api

import kotlin.test.Test
import kotlin.test.assertEquals

public class PeerLifecycleIntegrationTest {
    @Test
    public fun publishPeers_updatesPeerFlowAndEmitsPeerDiagnostics(): Unit {
        // Arrange
        val diagnosticSink = DiagnosticSink.create(
            bufferSize = 8,
            clock = { 88L },
        )
        val stubApi = MeshLink.create(
            config = MeshLinkConfig.default(),
            diagnosticSink = diagnosticSink,
        ) as StubMeshLinkApi
        val discoveredPeer = PeerDetail(
            peerId = PeerIdHex(value = "00112233"),
            state = PeerState.Discovered,
            displayName = "Alice",
            lastSeenEpochMillis = 100L,
        )
        val connectedPeer = discoveredPeer.copy(state = PeerState.Connected)

        // Act
        stubApi.publishPeers(peerDetails = listOf(discoveredPeer))
        stubApi.publishPeers(peerDetails = listOf(connectedPeer))
        val actualPeers = stubApi.peers.value
        val actualStates = stubApi.diagnosticEvents.replayCache.map { event ->
            (event.payload as DiagnosticPayload.PeerLifecycle).state
        }

        // Assert
        assertEquals(expected = listOf(connectedPeer), actual = actualPeers)
        assertEquals(
            expected = listOf(PeerState.Discovered, PeerState.Connected),
            actual = actualStates,
        )
    }
}
