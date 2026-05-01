package ch.trancee.meshlink.power

/** Transfer activity state for a managed connection. */
public enum class TransferStatus {
  IDLE,
  IN_FLIGHT,
  COMPLETE,
}
