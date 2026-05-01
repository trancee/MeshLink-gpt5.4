package ch.trancee.meshlink.transport

import ch.trancee.meshlink.api.PeerIdHex

/** Deterministic tie-breaker for deciding which side initiates a connection. */
public object ConnectionInitiationPolicy {
  /**
   * Returns true when the local peer should initiate the connection.
   *
   * Lexicographic ordering ensures both peers make the same decision independently.
   */
  public fun shouldLocalPeerInitiate(localPeerId: PeerIdHex, remotePeerId: PeerIdHex): Boolean {
    val normalizedLocalPeerId: String = localPeerId.value.lowercase()
    val normalizedRemotePeerId: String = remotePeerId.value.lowercase()

    if (normalizedLocalPeerId == normalizedRemotePeerId) {
      return false
    }

    return normalizedLocalPeerId < normalizedRemotePeerId
  }
}
