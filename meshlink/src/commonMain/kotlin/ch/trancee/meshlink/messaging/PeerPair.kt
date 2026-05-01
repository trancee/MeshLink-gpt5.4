package ch.trancee.meshlink.messaging

import ch.trancee.meshlink.api.PeerIdHex

/** Sender/recipient pair used as a rate-limiting key. */
public data class PeerPair(
  public val senderPeerId: PeerIdHex,
  public val recipientPeerId: PeerIdHex,
)
