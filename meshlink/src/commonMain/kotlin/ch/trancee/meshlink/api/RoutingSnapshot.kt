package ch.trancee.meshlink.api

public data class RoutingSnapshot(
    public val destinationPeerId: PeerIdHex,
    public val nextHopPeerId: PeerIdHex,
    public val hopCount: Int,
    public val metric: Int,
)
