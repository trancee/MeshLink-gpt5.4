package ch.trancee.meshlink.api

/** Peer lifecycle as observed by the local node. */
public sealed interface PeerState {
  /** Peer has been discovered but not yet connected. */
  public data object Discovered : PeerState

  /** Peer connection or handshake is in progress. */
  public data object Connecting : PeerState

  /** Peer is connected and available for traffic. */
  public data object Connected : PeerState

  /** Peer was previously known but is no longer connected. */
  public data object Disconnected : PeerState

  /** Peer interaction failed with a reason suitable for diagnostics or UI. */
  public data class Failed(public val reason: String) : PeerState
}
