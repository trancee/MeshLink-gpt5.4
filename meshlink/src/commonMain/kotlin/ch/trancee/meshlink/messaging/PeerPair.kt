package ch.trancee.meshlink.messaging

import ch.trancee.meshlink.api.PeerIdHex

public data class PeerPair(
  public val senderPeerId: PeerIdHex,
  public val recipientPeerId: PeerIdHex,
)
