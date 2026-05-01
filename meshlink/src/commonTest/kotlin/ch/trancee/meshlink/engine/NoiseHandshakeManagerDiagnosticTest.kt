package ch.trancee.meshlink.engine

import ch.trancee.meshlink.api.DiagnosticCode
import ch.trancee.meshlink.api.DiagnosticPayload
import ch.trancee.meshlink.api.DiagnosticSink
import ch.trancee.meshlink.api.PeerIdHex
import ch.trancee.meshlink.api.PeerState
import ch.trancee.meshlink.crypto.noise.HandshakeRole
import ch.trancee.meshlink.wire.messages.HandshakeMessage
import ch.trancee.meshlink.wire.messages.HandshakeRound
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

public class NoiseHandshakeManagerDiagnosticTest {
    @Test
    public fun managers_completeTheThreeMessageHandshakeAndEmitStartedAndSucceededDiagnostics(): Unit {
        // Arrange
        val peerId = PeerIdHex(value = "00112233")
        val initiatorSink = DiagnosticSink.create(bufferSize = 8, clock = { 10L })
        val responderSink = DiagnosticSink.create(bufferSize = 8, clock = { 20L })
        val initiator = NoiseHandshakeManager(diagnosticSink = initiatorSink)
        val responder = NoiseHandshakeManager(diagnosticSink = responderSink)

        // Act
        val first: HandshakeMessage = initiator.beginHandshake(
            peerId = peerId,
            role = HandshakeRole.INITIATOR,
            payload = byteArrayOf(0x01),
        )
        responder.receiveHandshakeMessage(
            peerId = peerId,
            role = HandshakeRole.RESPONDER,
            message = first,
        )
        val second: HandshakeMessage = responder.createOutboundMessage(
            peerId = peerId,
            payload = byteArrayOf(0x02),
        )
        initiator.receiveHandshakeMessage(
            peerId = peerId,
            role = HandshakeRole.INITIATOR,
            message = second,
        )
        val third: HandshakeMessage = initiator.createOutboundMessage(
            peerId = peerId,
            payload = byteArrayOf(0x03),
        )
        responder.receiveHandshakeMessage(
            peerId = peerId,
            role = HandshakeRole.RESPONDER,
            message = third,
        )

        // Assert
        assertEquals(expected = HandshakeRound.ONE, actual = first.round)
        assertEquals(expected = HandshakeRound.TWO, actual = second.round)
        assertEquals(expected = HandshakeRound.THREE, actual = third.round)
        assertFalse(actual = initiator.isHandshakeActive(peerId = peerId))
        assertFalse(actual = responder.isHandshakeActive(peerId = peerId))
        assertEquals(
            expected = listOf(DiagnosticCode.HANDSHAKE_STARTED, DiagnosticCode.HANDSHAKE_SUCCEEDED),
            actual = initiatorSink.diagnosticEvents.replayCache.map { event -> event.code },
        )
        assertEquals(
            expected = listOf(DiagnosticCode.HANDSHAKE_STARTED, DiagnosticCode.HANDSHAKE_SUCCEEDED),
            actual = responderSink.diagnosticEvents.replayCache.map { event -> event.code },
        )
        val initiatorPayload = assertIs<DiagnosticPayload.PeerLifecycle>(initiatorSink.diagnosticEvents.replayCache.last().payload)
        val responderPayload = assertIs<DiagnosticPayload.PeerLifecycle>(responderSink.diagnosticEvents.replayCache.last().payload)
        assertEquals(expected = peerId, actual = initiatorPayload.peerId)
        assertEquals(expected = PeerState.Connected, actual = initiatorPayload.state)
        assertEquals(expected = peerId, actual = responderPayload.peerId)
        assertEquals(expected = PeerState.Connected, actual = responderPayload.state)
    }

    @Test
    public fun receiveHandshakeMessage_createsResponderStateLazilyOnFirstInboundMessage(): Unit {
        // Arrange
        val peerId = PeerIdHex(value = "00112233")
        val sink = DiagnosticSink.create(bufferSize = 4, clock = { 30L })
        val manager = NoiseHandshakeManager(diagnosticSink = sink)

        // Act
        manager.receiveHandshakeMessage(
            peerId = peerId,
            role = HandshakeRole.RESPONDER,
            message = HandshakeMessage(
                round = HandshakeRound.ONE,
                payload = byteArrayOf(0x11),
            ),
        )

        // Assert
        assertTrue(actual = manager.isHandshakeActive(peerId = peerId))
        assertEquals(
            expected = listOf(DiagnosticCode.HANDSHAKE_STARTED),
            actual = sink.diagnosticEvents.replayCache.map { event -> event.code },
        )
        val payload = assertIs<DiagnosticPayload.PeerLifecycle>(sink.diagnosticEvents.replayCache.single().payload)
        assertEquals(expected = PeerState.Connecting, actual = payload.state)
    }

    @Test
    public fun beginHandshake_rejectsPeersThatAlreadyHaveAnActiveHandshake(): Unit {
        // Arrange
        val peerId = PeerIdHex(value = "00112233")
        val manager = NoiseHandshakeManager()
        manager.beginHandshake(
            peerId = peerId,
            role = HandshakeRole.INITIATOR,
            payload = byteArrayOf(0x01),
        )

        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            manager.beginHandshake(
                peerId = peerId,
                role = HandshakeRole.INITIATOR,
                payload = byteArrayOf(0x02),
            )
        }

        // Assert
        assertEquals(
            expected = "NoiseHandshakeManager already has an active handshake for 00112233.",
            actual = error.message,
        )
    }

    @Test
    public fun createOutboundMessage_rejectsPeersWithoutAnActiveHandshake(): Unit {
        // Arrange
        val manager = NoiseHandshakeManager()
        val peerId = PeerIdHex(value = "00112233")

        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            manager.createOutboundMessage(
                peerId = peerId,
                payload = byteArrayOf(0x01),
            )
        }

        // Assert
        assertEquals(
            expected = "NoiseHandshakeManager has no active handshake for 00112233.",
            actual = error.message,
        )
    }

    @Test
    public fun receiveHandshakeMessage_emitsFailureDiagnosticsAndClearsStateOnProtocolErrors(): Unit {
        // Arrange
        val peerId = PeerIdHex(value = "00112233")
        val sink = DiagnosticSink.create(bufferSize = 4, clock = { 40L })
        val manager = NoiseHandshakeManager(diagnosticSink = sink)

        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            manager.receiveHandshakeMessage(
                peerId = peerId,
                role = HandshakeRole.RESPONDER,
                message = HandshakeMessage(
                    round = HandshakeRound.TWO,
                    payload = byteArrayOf(0x55),
                ),
            )
        }

        // Assert
        assertEquals(
            expected = "NoiseXXHandshake expected ONE but received TWO for RESPONDER.",
            actual = error.message,
        )
        assertFalse(actual = manager.isHandshakeActive(peerId = peerId))
        assertEquals(
            expected = listOf(DiagnosticCode.HANDSHAKE_STARTED, DiagnosticCode.HANDSHAKE_FAILED),
            actual = sink.diagnosticEvents.replayCache.map { event -> event.code },
        )
        val payload = assertIs<DiagnosticPayload.HandshakeFailure>(sink.diagnosticEvents.replayCache.last().payload)
        assertEquals(expected = peerId, actual = payload.peerId)
        assertEquals(expected = error.toString(), actual = payload.reason)
    }

    @Test
    public fun createOutboundMessage_emitsFailureDiagnosticsAndClearsStateOnRoleViolations(): Unit {
        // Arrange
        val peerId = PeerIdHex(value = "00112233")
        val sink = DiagnosticSink.create(bufferSize = 4, clock = { 50L })
        val manager = NoiseHandshakeManager(diagnosticSink = sink)
        manager.receiveHandshakeMessage(
            peerId = peerId,
            role = HandshakeRole.RESPONDER,
            message = HandshakeMessage(
                round = HandshakeRound.ONE,
                payload = byteArrayOf(0x22),
            ),
        )
        manager.createOutboundMessage(
            peerId = peerId,
            payload = byteArrayOf(0x23),
        )

        // Act
        val error = assertFailsWith<IllegalStateException> {
            manager.createOutboundMessage(
                peerId = peerId,
                payload = byteArrayOf(0x24),
            )
        }

        // Assert
        assertEquals(
            expected = "NoiseXXHandshake responder cannot send in round 3.",
            actual = error.message,
        )
        assertFalse(actual = manager.isHandshakeActive(peerId = peerId))
        assertEquals(
            expected = listOf(DiagnosticCode.HANDSHAKE_STARTED, DiagnosticCode.HANDSHAKE_FAILED),
            actual = sink.diagnosticEvents.replayCache.map { event -> event.code },
        )
        val payload = assertIs<DiagnosticPayload.HandshakeFailure>(sink.diagnosticEvents.replayCache.last().payload)
        assertEquals(expected = peerId, actual = payload.peerId)
        assertEquals(expected = error.toString(), actual = payload.reason)
    }
}
