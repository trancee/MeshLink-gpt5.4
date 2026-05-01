package ch.trancee.meshlink.api

public sealed interface PeerState {
  public data object Discovered : PeerState

  public data object Connecting : PeerState

  public data object Connected : PeerState

  public data object Disconnected : PeerState

  public data class Failed(public val reason: String) : PeerState
}
