package ch.trancee.meshlink.api

public data class PeerDetail(
    public val peerId: PeerIdHex,
    public val state: PeerState,
    public val displayName: String?,
    public val lastSeenEpochMillis: Long,
)
