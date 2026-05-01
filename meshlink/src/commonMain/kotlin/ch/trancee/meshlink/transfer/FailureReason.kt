package ch.trancee.meshlink.transfer

/** Canonical failure reasons for transfer termination. */
public enum class FailureReason {
  TIMEOUT,
  CANCELLED,
  DISCONNECTED,
  REMOTE_REJECTED,
}
