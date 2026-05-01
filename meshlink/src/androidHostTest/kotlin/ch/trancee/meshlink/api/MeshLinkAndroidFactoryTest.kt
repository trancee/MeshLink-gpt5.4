package ch.trancee.meshlink.api

import kotlin.test.Test
import kotlin.test.assertEquals

public class MeshLinkAndroidFactoryTest {
    @Test
    public fun create_withoutArgumentsReturnsAnUninitializedApi(): Unit {
        // Arrange
        // Act
        val actual = MeshLinkAndroidFactory.create()

        // Assert
        assertEquals(expected = MeshLinkState.UNINITIALIZED, actual = actual.state.value)
    }

    @Test
    public fun create_withConfigUsesTheConfiguredDiagnosticRedaction(): Unit {
        // Arrange
        val config = MeshLinkConfig {
            diagnostics {
                bufferSize = 1
                redactPeerIds = true
            }
        }
        val api = MeshLinkAndroidFactory.create(config = config)

        // Act
        api.send(
            peerId = PeerIdHex(value = "0011223344556677"),
            payload = byteArrayOf(0x01),
        )
        val actual = api.diagnosticEvents.replayCache.single().payload as DiagnosticPayload.PeerLifecycle

        // Assert
        assertEquals(expected = "00112233", actual = actual.peerId.value)
        assertEquals(expected = PeerState.Connected, actual = actual.state)
    }
}
