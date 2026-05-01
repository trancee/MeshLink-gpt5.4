package ch.trancee.meshlink.crypto.noise

public class HandshakeState(
    private val role: HandshakeRole,
) {
    private var step: Int = 0

    public fun currentRound(): Int = step + 1

    public fun isComplete(): Boolean = step >= LAST_STEP_INDEX

    public fun canSend(): Boolean {
        return when (role) {
            HandshakeRole.INITIATOR -> step == 0 || step == 2
            HandshakeRole.RESPONDER -> step == 1
        }
    }

    public fun canReceive(): Boolean {
        return when (role) {
            HandshakeRole.INITIATOR -> step == 1
            HandshakeRole.RESPONDER -> step == 0 || step == 2
        }
    }

    public fun recordSend(): Unit {
        if (!canSend()) {
            throw IllegalStateException("HandshakeState cannot send in round ${currentRound()} for role ${role.name}.")
        }
        step += 1
    }

    public fun recordReceive(): Unit {
        if (!canReceive()) {
            throw IllegalStateException("HandshakeState cannot receive in round ${currentRound()} for role ${role.name}.")
        }
        step += 1
    }

    public companion object {
        private const val LAST_STEP_INDEX: Int = 3
    }
}
