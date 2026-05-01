package ch.trancee.meshlink.engine

import ch.trancee.meshlink.api.DiagnosticCode
import ch.trancee.meshlink.api.DiagnosticPayload
import ch.trancee.meshlink.api.DiagnosticSink
import ch.trancee.meshlink.api.NoOpDiagnosticSink
import ch.trancee.meshlink.api.PeerIdHex
import ch.trancee.meshlink.api.PeerState
import ch.trancee.meshlink.crypto.noise.HandshakeRole
import ch.trancee.meshlink.crypto.noise.NoiseXXHandshake
import ch.trancee.meshlink.wire.messages.HandshakeMessage

public class NoiseHandshakeManager(
    private val diagnosticSink: DiagnosticSink = NoOpDiagnosticSink,
) {
    private val handshakes: MutableMap<String, NoiseXXHandshake> = mutableMapOf()

    public fun beginHandshake(
        peerId: PeerIdHex,
        role: HandshakeRole,
        payload: ByteArray,
    ): HandshakeMessage {
        require(peerId.value !in handshakes) {
            "NoiseHandshakeManager already has an active handshake for ${peerId.value}."
        }

        val handshake = NoiseXXHandshake(role = role)
        handshakes[peerId.value] = handshake
        emitHandshakeStarted(peerId = peerId)
        return createOutboundMessage(peerId = peerId, payload = payload)
    }

    public fun receiveHandshakeMessage(
        peerId: PeerIdHex,
        role: HandshakeRole,
        message: HandshakeMessage,
    ): Unit {
        val handshake: NoiseXXHandshake = handshakes.getOrPut(peerId.value) {
            emitHandshakeStarted(peerId = peerId)
            NoiseXXHandshake(role = role)
        }

        runCatching {
            handshake.receiveInboundMessage(message = message)
        }.onFailure { throwable ->
            handshakes.remove(peerId.value)
            emitHandshakeFailed(peerId = peerId, reason = throwable.toString())
            throw throwable
        }

        if (handshake.isComplete()) {
            handshakes.remove(peerId.value)
            emitHandshakeSucceeded(peerId = peerId)
        }
    }

    public fun createOutboundMessage(
        peerId: PeerIdHex,
        payload: ByteArray,
    ): HandshakeMessage {
        val handshake: NoiseXXHandshake = requireNotNull(handshakes[peerId.value]) {
            "NoiseHandshakeManager has no active handshake for ${peerId.value}."
        }

        return runCatching {
            handshake.createOutboundMessage(payload = payload)
        }.onFailure { throwable ->
            handshakes.remove(peerId.value)
            emitHandshakeFailed(peerId = peerId, reason = throwable.toString())
            throw throwable
        }.getOrThrow().also {
            if (handshake.isComplete()) {
                handshakes.remove(peerId.value)
                emitHandshakeSucceeded(peerId = peerId)
            }
        }
    }

    public fun isHandshakeActive(peerId: PeerIdHex): Boolean {
        return peerId.value in handshakes
    }

    private fun emitHandshakeStarted(peerId: PeerIdHex): Unit {
        diagnosticSink.emit(code = DiagnosticCode.HANDSHAKE_STARTED) {
            DiagnosticPayload.PeerLifecycle(
                peerId = peerId,
                state = PeerState.Connecting,
            )
        }
    }

    private fun emitHandshakeSucceeded(peerId: PeerIdHex): Unit {
        diagnosticSink.emit(code = DiagnosticCode.HANDSHAKE_SUCCEEDED) {
            DiagnosticPayload.PeerLifecycle(
                peerId = peerId,
                state = PeerState.Connected,
            )
        }
    }

    private fun emitHandshakeFailed(
        peerId: PeerIdHex,
        reason: String,
    ): Unit {
        diagnosticSink.emit(code = DiagnosticCode.HANDSHAKE_FAILED) {
            DiagnosticPayload.HandshakeFailure(
                peerId = peerId,
                reason = reason,
            )
        }
    }
}
