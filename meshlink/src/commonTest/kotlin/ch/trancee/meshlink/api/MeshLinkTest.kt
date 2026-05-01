package ch.trancee.meshlink.api

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

public class MeshLinkTest {
    @Test
    public fun create_withoutArgumentsReturnsAnUninitializedApi(): Unit {
        // Arrange
        // Act
        val actual: MeshLinkApi = MeshLink.create()

        // Assert
        assertEquals(expected = MeshLinkState.UNINITIALIZED, actual = actual.state.value)
        assertTrue(actual = actual.peers.value.isEmpty())
        assertTrue(actual = actual.messages.replayCache.isEmpty())
    }

    @Test
    public fun create_withCustomSinkMapsLifecycleAndMessageFlows(): Unit {
        // Arrange
        val diagnosticSink = DiagnosticSink.create(
            bufferSize = 8,
            clock = { 77L },
        )
        val payload = byteArrayOf(0x01, 0x02, 0x03)
        val api = MeshLink.create(
            config = MeshLinkConfig.default(),
            diagnosticSink = diagnosticSink,
        )

        // Act
        api.start()
        api.send(
            peerId = PeerIdHex(value = "00112233"),
            payload = payload,
        )
        val actualMessage = api.messages.replayCache.single()
        val diagnosticCodes = api.diagnosticEvents.replayCache.map { event -> event.code }

        // Assert
        assertEquals(expected = MeshLinkState.RUNNING, actual = api.state.value)
        assertContentEquals(expected = payload, actual = actualMessage)
        assertEquals(
            expected = listOf(DiagnosticCode.ENGINE_STARTED, DiagnosticCode.MESSAGE_SENT),
            actual = diagnosticCodes,
        )
        val lastPayload = assertIs<DiagnosticPayload.PeerLifecycle>(api.diagnosticEvents.replayCache.last().payload)
        assertEquals(expected = "00112233", actual = lastPayload.peerId.value)
        assertEquals(expected = PeerState.Connected, actual = lastPayload.state)
    }

    @Test
    public fun create_withConfigBuildsSinkFromDiagnosticSettings(): Unit {
        // Arrange
        val config = MeshLinkConfig {
            diagnostics {
                bufferSize = 1
                redactPeerIds = true
            }
        }
        val api = MeshLink.create(config = config)

        // Act
        api.send(
            peerId = PeerIdHex(value = "0011223344556677"),
            payload = byteArrayOf(0x09),
        )
        val actualPayload = api.diagnosticEvents.replayCache.single().payload

        // Assert
        val peerLifecycle = assertIs<DiagnosticPayload.PeerLifecycle>(actualPayload)
        assertEquals(expected = "00112233", actual = peerLifecycle.peerId.value)
    }
}
