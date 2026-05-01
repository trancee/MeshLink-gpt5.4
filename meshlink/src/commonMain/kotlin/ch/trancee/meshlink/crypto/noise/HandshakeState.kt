package ch.trancee.meshlink.crypto.noise

/**
 * Tracks turn-taking for the three-message Noise XX handshake.
 *
 * The implementation stores a simple step index rather than explicit round names so callers can ask
 * whether the local role is allowed to send or receive next.
 */
public class HandshakeState(private val role: HandshakeRole) {
  private var step: Int = 0

  /** Human-facing handshake round number starting at 1. */
  public fun currentRound(): Int = step + 1

  /** Returns true once all Noise XX messages have been processed. */
  public fun isComplete(): Boolean = step >= LAST_STEP_INDEX

  /** Whether the current role owns the next outbound handshake turn. */
  public fun canSend(): Boolean {
    return when (role) {
      HandshakeRole.INITIATOR -> step == 0 || step == 2
      HandshakeRole.RESPONDER -> step == 1
    }
  }

  /** Whether the current role expects the next inbound handshake message. */
  public fun canReceive(): Boolean {
    return when (role) {
      HandshakeRole.INITIATOR -> step == 1
      HandshakeRole.RESPONDER -> step == 0 || step == 2
    }
  }

  /** Advances the state machine after a successful outbound handshake message. */
  public fun recordSend(): Unit {
    if (!canSend()) {
      throw IllegalStateException(
        "HandshakeState cannot send in round ${currentRound()} for role ${role.name}."
      )
    }
    step += 1
  }

  /** Advances the state machine after a successful inbound handshake message. */
  public fun recordReceive(): Unit {
    if (!canReceive()) {
      throw IllegalStateException(
        "HandshakeState cannot receive in round ${currentRound()} for role ${role.name}."
      )
    }
    step += 1
  }

  public companion object {
    private const val LAST_STEP_INDEX: Int = 3
  }
}
