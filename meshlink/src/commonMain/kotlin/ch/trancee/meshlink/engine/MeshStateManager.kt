package ch.trancee.meshlink.engine

import ch.trancee.meshlink.api.PeerIdHex

public class MeshStateManager(
    private val stalePeerTimeoutMillis: Long = DEFAULT_STALE_PEER_TIMEOUT_MILLIS,
) {
    init {
        require(stalePeerTimeoutMillis > 0) {
            "MeshStateManager stalePeerTimeoutMillis must be greater than 0."
        }
    }

    public fun sweep(
        peers: List<ManagedPeer>,
        routes: List<ManagedRoute>,
        nowEpochMillis: Long,
    ): MeshStateSweepResult {
        require(nowEpochMillis >= 0) {
            "MeshStateManager nowEpochMillis must be greater than or equal to 0."
        }

        val stalePeers: List<PeerIdHex> = peers.filter { peer ->
            nowEpochMillis - peer.lastSeenEpochMillis >= stalePeerTimeoutMillis
        }.map { peer -> peer.peerId }

        val expiredRoutes: List<PeerIdHex> = routes.filter { route ->
            route.expiresAtEpochMillis <= nowEpochMillis
        }.map { route -> route.destinationPeerId }

        return MeshStateSweepResult(
            stalePeers = stalePeers,
            expiredRoutes = expiredRoutes,
        )
    }

    public companion object {
        public const val DEFAULT_STALE_PEER_TIMEOUT_MILLIS: Long = 30_000L
    }
}

public data class ManagedPeer(
    public val peerId: PeerIdHex,
    public val lastSeenEpochMillis: Long,
)

public data class ManagedRoute(
    public val destinationPeerId: PeerIdHex,
    public val expiresAtEpochMillis: Long,
)

public data class MeshStateSweepResult(
    public val stalePeers: List<PeerIdHex>,
    public val expiredRoutes: List<PeerIdHex>,
)
