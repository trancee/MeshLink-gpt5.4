package ch.trancee.meshlink.api

/** High-level lifecycle states for a MeshLink runtime. */
public enum class MeshLinkState {
  /** Runtime has been created but not started yet. */
  UNINITIALIZED,

  /** Runtime is actively advertising, scanning, and processing mesh work. */
  RUNNING,

  /** Runtime is temporarily suspended but may resume without full recreation. */
  PAUSED,

  /** Runtime has been stopped in an orderly way. */
  STOPPED,

  /** Runtime encountered an issue that may be recoverable through restart/resume logic. */
  RECOVERABLE,

  /** Runtime reached a fatal state and must not transition further. */
  TERMINAL;

  /** Returns whether a direct transition to [target] is allowed. */
  public fun canTransitionTo(target: MeshLinkState): Boolean {
    return when (this) {
      UNINITIALIZED -> target == RUNNING || target == TERMINAL
      RUNNING ->
        target == PAUSED || target == STOPPED || target == RECOVERABLE || target == TERMINAL
      PAUSED -> target == RUNNING || target == STOPPED || target == TERMINAL
      STOPPED -> target == RUNNING || target == TERMINAL
      RECOVERABLE -> target == RUNNING || target == STOPPED || target == TERMINAL
      TERMINAL -> false
    }
  }
}
