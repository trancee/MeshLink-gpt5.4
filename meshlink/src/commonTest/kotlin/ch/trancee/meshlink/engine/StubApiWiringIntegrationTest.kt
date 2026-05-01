package ch.trancee.meshlink.engine

import ch.trancee.meshlink.api.DiagnosticCode
import ch.trancee.meshlink.api.MeshLink
import ch.trancee.meshlink.api.MeshLinkState
import ch.trancee.meshlink.transport.VirtualMeshTransport
import ch.trancee.meshlink.api.PeerIdHex
import kotlin.test.Test
import kotlin.test.assertEquals

public class StubApiWiringIntegrationTest {
    @Test
    public fun stubApiAndMeshEngine_shareTheSameLifecycleContract(): Unit {
        // Arrange
        val config = ch.trancee.meshlink.api.MeshLinkConfig.default()
        val stubApi = MeshLink.create(config = config)
        val engine = MeshEngine.create(
            config = MeshEngineConfig.default().copy(meshLinkConfig = config),
            transport = VirtualMeshTransport(localPeerId = PeerIdHex(value = "00112233")),
            cryptoProvider = FakeCryptoProvider(),
        )

        // Act
        stubApi.start()
        engine.start()
        stubApi.pause()
        engine.pause()
        stubApi.resume()
        engine.resume()
        stubApi.stop()
        engine.stop()

        // Assert
        assertEquals(expected = MeshLinkState.STOPPED, actual = stubApi.state.value)
        assertEquals(expected = MeshLinkState.STOPPED, actual = engine.state.value)
        assertEquals(
            expected = listOf(
                DiagnosticCode.ENGINE_STARTED,
                DiagnosticCode.ENGINE_PAUSED,
                DiagnosticCode.ENGINE_RESUMED,
                DiagnosticCode.ENGINE_STOPPED,
            ),
            actual = stubApi.diagnosticEvents.replayCache.map { event -> event.code },
        )
        assertEquals(
            expected = listOf(
                DiagnosticCode.ENGINE_STARTED,
                DiagnosticCode.ENGINE_PAUSED,
                DiagnosticCode.ENGINE_RESUMED,
                DiagnosticCode.ENGINE_STOPPED,
            ),
            actual = engine.diagnosticEvents.replayCache.map { event -> event.code },
        )
    }
}
