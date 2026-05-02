package ch.trancee.meshlink.messaging

/** Reason why a send was queued instead of transmitted immediately. */
public enum class QueuedReason {
  RATE_LIMITED,
  BUFFER_FULL,
  ROUTE_UNAVAILABLE,
}
