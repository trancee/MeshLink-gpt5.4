package ch.trancee.meshlink.api

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

public class StubApiWiringIntegrationTest {
    @Test
    public fun stubApi_wiresLifecycleMethodsToStateAndDiagnostics(): Unit {
        // Arrange
        val diagnosticSink = DiagnosticSink.create(
            bufferSize = 8,
            clock = { 55L },
        )
        val api = MeshLink.create(
            config = MeshLinkConfig.default(),
            diagnosticSink = diagnosticSink,
        )

        // Act
        api.start()
        api.pause()
        api.resume()
        api.stop()
        val actualStates = api.state.value
        val actualCodes = api.diagnosticEvents.replayCache.map { event -> event.code }

        // Assert
        assertEquals(expected = MeshLinkState.STOPPED, actual = actualStates)
        assertEquals(
            expected = listOf(
                DiagnosticCode.ENGINE_STARTED,
                DiagnosticCode.ENGINE_PAUSED,
                DiagnosticCode.ENGINE_RESUMED,
                DiagnosticCode.ENGINE_STOPPED,
            ),
            actual = actualCodes,
        )
    }

    @Test
    public fun stubApi_supportsPublishedIncomingMessages(): Unit {
        // Arrange
        val stubApi = MeshLink.create(
            config = MeshLinkConfig.default(),
            diagnosticSink = NoOpDiagnosticSink,
        ) as StubMeshLinkApi
        val payload = byteArrayOf(0x0A, 0x0B)

        // Act
        stubApi.publishIncomingMessage(payload = payload)
        val actual = stubApi.messages.replayCache.single()

        // Assert
        assertContentEquals(expected = payload, actual = actual)
    }
}
