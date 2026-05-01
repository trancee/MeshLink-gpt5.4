package ch.trancee.meshlink.messaging

import ch.trancee.meshlink.api.PeerIdHex

public data class MessageIdKey(
  public val senderPeerId: PeerIdHex,
  public val sequenceNumber: Long,
) {
  init {
    require(sequenceNumber >= 0) {
      "MessageIdKey sequenceNumber must be greater than or equal to 0."
    }
  }
}
