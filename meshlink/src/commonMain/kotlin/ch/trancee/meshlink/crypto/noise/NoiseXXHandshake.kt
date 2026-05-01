package ch.trancee.meshlink.crypto.noise

import ch.trancee.meshlink.wire.messages.HandshakeMessage
import ch.trancee.meshlink.wire.messages.HandshakeRound

public class NoiseXXHandshake(
    private val role: HandshakeRole,
) {
    private val state: HandshakeState = HandshakeState(role = role)

    public fun createOutboundMessage(payload: ByteArray): HandshakeMessage {
        val round: HandshakeRound = expectedOutboundRound()
        state.recordSend()
        return HandshakeMessage(
            round = round,
            payload = payload.copyOf(),
        )
    }

    public fun receiveInboundMessage(message: HandshakeMessage): Unit {
        val expectedRound: HandshakeRound = expectedInboundRound()
        if (message.round != expectedRound) {
            throw IllegalArgumentException(
                "NoiseXXHandshake expected ${expectedRound.name} but received ${message.round.name} for ${role.name}.",
            )
        }
        state.recordReceive()
    }

    public fun isComplete(): Boolean = state.isComplete()

    private fun expectedOutboundRound(): HandshakeRound {
        return when (role) {
            HandshakeRole.INITIATOR -> when (state.currentRound()) {
                1 -> HandshakeRound.ONE
                3 -> HandshakeRound.THREE
                else -> throw IllegalStateException("NoiseXXHandshake initiator cannot send in round ${state.currentRound()}.")
            }
            HandshakeRole.RESPONDER -> when (state.currentRound()) {
                2 -> HandshakeRound.TWO
                else -> throw IllegalStateException("NoiseXXHandshake responder cannot send in round ${state.currentRound()}.")
            }
        }
    }

    private fun expectedInboundRound(): HandshakeRound {
        return when (role) {
            HandshakeRole.INITIATOR -> when (state.currentRound()) {
                2 -> HandshakeRound.TWO
                else -> throw IllegalStateException("NoiseXXHandshake initiator cannot receive in round ${state.currentRound()}.")
            }
            HandshakeRole.RESPONDER -> when (state.currentRound()) {
                1 -> HandshakeRound.ONE
                3 -> HandshakeRound.THREE
                else -> throw IllegalStateException("NoiseXXHandshake responder cannot receive in round ${state.currentRound()}.")
            }
        }
    }
}
