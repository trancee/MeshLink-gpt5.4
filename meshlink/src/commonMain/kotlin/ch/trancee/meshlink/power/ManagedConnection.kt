package ch.trancee.meshlink.power

public data class ManagedConnection(
  public val peerKey: PeerKey,
  public val transferStatus: TransferStatus,
  public val lastActivityEpochMillis: Long,
) {
  init {
    require(lastActivityEpochMillis >= 0) {
      "ManagedConnection lastActivityEpochMillis must be greater than or equal to 0."
    }
  }
}
