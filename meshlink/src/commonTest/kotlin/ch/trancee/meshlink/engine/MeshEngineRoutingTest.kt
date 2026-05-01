package ch.trancee.meshlink.engine

import ch.trancee.meshlink.api.DiagnosticCode
import ch.trancee.meshlink.api.DiagnosticSink
import ch.trancee.meshlink.api.PeerIdHex
import ch.trancee.meshlink.crypto.noise.HandshakeRole
import ch.trancee.meshlink.transport.VirtualMeshTransport
import ch.trancee.meshlink.wire.messages.BroadcastMessage
import ch.trancee.meshlink.wire.messages.HandshakeRound
import ch.trancee.meshlink.wire.messages.HelloMessage
import ch.trancee.meshlink.wire.messages.RoutedMessage
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

public class MeshEngineRoutingTest {
    @Test
    public fun receiveInboundMessage_routesRoutedPayloadsToThePublicMessageFlow(): Unit {
        // Arrange
        val engine = MeshEngine.create(
            config = MeshEngineConfig.default(),
            transport = VirtualMeshTransport(localPeerId = PeerIdHex(value = "00112233")),
            cryptoProvider = FakeCryptoProvider(),
        )
        val payload = byteArrayOf(0x01, 0x02)

        // Act
        engine.receiveInboundMessage(
            peerId = PeerIdHex(value = "44556677"),
            message = RoutedMessage(hopCount = 1u, maxHops = 3u, payload = payload),
        )
        val actual = engine.messages.replayCache.single()

        // Assert
        assertContentEquals(expected = payload, actual = actual)
    }

    @Test
    public fun receiveInboundMessage_routesBroadcastPayloadsToThePublicMessageFlow(): Unit {
        // Arrange
        val engine = MeshEngine.create(
            config = MeshEngineConfig.default(),
            transport = VirtualMeshTransport(localPeerId = PeerIdHex(value = "00112233")),
            cryptoProvider = FakeCryptoProvider(),
        )
        val payload = byteArrayOf(0x03, 0x04)

        // Act
        engine.receiveInboundMessage(
            peerId = PeerIdHex(value = "44556677"),
            message = BroadcastMessage(
                originPeerId = byteArrayOf(0x01),
                sequenceNumber = 7,
                maxHops = 4u,
                payload = payload,
            ),
        )
        val actual = engine.messages.replayCache.single()

        // Assert
        assertContentEquals(expected = payload, actual = actual)
    }

    @Test
    public fun receiveInboundMessage_routesHandshakeMessagesToTheHandshakeManager(): Unit {
        // Arrange
        val diagnosticSink = DiagnosticSink.create(bufferSize = 4, clock = { 99L })
        val engine = MeshEngine.create(
            config = MeshEngineConfig.default(),
            transport = VirtualMeshTransport(localPeerId = PeerIdHex(value = "00112233")),
            diagnosticSink = diagnosticSink,
            cryptoProvider = FakeCryptoProvider(),
        )
        val peerId = PeerIdHex(value = "44556677")

        // Act
        engine.receiveInboundMessage(
            peerId = peerId,
            message = ch.trancee.meshlink.wire.messages.HandshakeMessage(
                round = HandshakeRound.ONE,
                payload = byteArrayOf(0x05),
            ),
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
    public fun receiveInboundMessage_ignoresMessagesThatAreNotHandshakeOrDataPayloads(): Unit {
        // Arrange
        val engine = MeshEngine.create(
            config = MeshEngineConfig.default(),
            transport = VirtualMeshTransport(localPeerId = PeerIdHex(value = "00112233")),
            cryptoProvider = FakeCryptoProvider(),
        )

        // Act
        engine.receiveInboundMessage(
            peerId = PeerIdHex(value = "44556677"),
            message = HelloMessage(peerId = byteArrayOf(0x01), appIdHash = 7),
        )

        // Assert
        assertTrue(actual = engine.messages.replayCache.isEmpty())
        assertFalse(actual = engine.handshakeManager.isHandshakeActive(peerId = PeerIdHex(value = "44556677")))
    }

    @Test
    public fun beginHandshakeAndContinueHandshake_delegateToTheHandshakeManager(): Unit {
        // Arrange
        val peerId = PeerIdHex(value = "44556677")
        val engine = MeshEngine.create(
            config = MeshEngineConfig.default(),
            transport = VirtualMeshTransport(localPeerId = PeerIdHex(value = "00112233")),
            cryptoProvider = FakeCryptoProvider(),
        )

        // Act
        val first = engine.beginHandshake(
            peerId = peerId,
            role = HandshakeRole.INITIATOR,
            payload = byteArrayOf(0x11),
        )
        engine.receiveInboundMessage(
            peerId = peerId,
            message = ch.trancee.meshlink.wire.messages.HandshakeMessage(
                round = HandshakeRound.TWO,
                payload = byteArrayOf(0x12),
            ),
            handshakeRole = HandshakeRole.INITIATOR,
        )
        val third = engine.continueHandshake(
            peerId = peerId,
            payload = byteArrayOf(0x13),
        )

        // Assert
        assertEquals(expected = HandshakeRound.ONE, actual = first.round)
        assertEquals(expected = HandshakeRound.THREE, actual = third.round)
        assertFalse(actual = engine.handshakeManager.isHandshakeActive(peerId = peerId))
    }
}
