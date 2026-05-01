package ch.trancee.meshlink.engine

import ch.trancee.meshlink.api.PeerIdHex

/** Sweeps peer and route state for expiration. */
public class MeshStateManager(
  private val stalePeerTimeoutMillis: Long = DEFAULT_STALE_PEER_TIMEOUT_MILLIS
) {
  init {
    require(stalePeerTimeoutMillis > 0) {
      "MeshStateManager stalePeerTimeoutMillis must be greater than 0."
    }
  }

  /** Returns the peers and routes that should be considered stale at [nowEpochMillis]. */
  public fun sweep(
    peers: List<ManagedPeer>,
    routes: List<ManagedRoute>,
    nowEpochMillis: Long,
  ): MeshStateSweepResult {
    require(nowEpochMillis >= 0) {
      "MeshStateManager nowEpochMillis must be greater than or equal to 0."
    }

    val stalePeers: List<PeerIdHex> =
      peers
        .filter { peer -> nowEpochMillis - peer.lastSeenEpochMillis >= stalePeerTimeoutMillis }
        .map { peer -> peer.peerId }

    val expiredRoutes: List<PeerIdHex> =
      routes
        .filter { route -> route.expiresAtEpochMillis <= nowEpochMillis }
        .map { route -> route.destinationPeerId }

    return MeshStateSweepResult(stalePeers = stalePeers, expiredRoutes = expiredRoutes)
  }

  public companion object {
    public const val DEFAULT_STALE_PEER_TIMEOUT_MILLIS: Long = 30_000L
  }
}

/** Peer tracked by the state sweeper. */
public data class ManagedPeer(public val peerId: PeerIdHex, public val lastSeenEpochMillis: Long)

/** Route tracked by the state sweeper. */
public data class ManagedRoute(
  public val destinationPeerId: PeerIdHex,
  public val expiresAtEpochMillis: Long,
)

/** Result of a mesh-state sweep. */
public data class MeshStateSweepResult(
  public val stalePeers: List<PeerIdHex>,
  public val expiredRoutes: List<PeerIdHex>,
)
