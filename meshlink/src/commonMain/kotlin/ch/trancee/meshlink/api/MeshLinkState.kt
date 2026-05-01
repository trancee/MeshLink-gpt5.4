package ch.trancee.meshlink.api

public enum class MeshLinkState {
    UNINITIALIZED,
    RUNNING,
    PAUSED,
    STOPPED,
    RECOVERABLE,
    TERMINAL,
    ;

    public fun canTransitionTo(target: MeshLinkState): Boolean {
        return when (this) {
            UNINITIALIZED -> target == RUNNING || target == TERMINAL
            RUNNING -> target == PAUSED || target == STOPPED || target == RECOVERABLE || target == TERMINAL
            PAUSED -> target == RUNNING || target == STOPPED || target == TERMINAL
            STOPPED -> target == RUNNING || target == TERMINAL
            RECOVERABLE -> target == RUNNING || target == STOPPED || target == TERMINAL
            TERMINAL -> false
        }
    }
}
