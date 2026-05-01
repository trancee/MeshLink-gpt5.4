package ch.trancee.meshlink.power

/** Stable key identifying a peer within power-management structures. */
public data class PeerKey(public val value: String) {
  init {
    require(value.isNotBlank()) { "PeerKey value must not be blank." }
  }
}
