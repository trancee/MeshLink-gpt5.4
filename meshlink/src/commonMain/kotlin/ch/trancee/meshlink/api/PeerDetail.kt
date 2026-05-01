package ch.trancee.meshlink.api

/** Snapshot of peer information published to API consumers. */
public data class PeerDetail(
  public val peerId: PeerIdHex,
  public val state: PeerState,
  public val displayName: String?,
  public val lastSeenEpochMillis: Long,
)
