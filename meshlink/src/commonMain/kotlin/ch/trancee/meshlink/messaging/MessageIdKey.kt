package ch.trancee.meshlink.messaging

import ch.trancee.meshlink.api.PeerIdHex

/** Stable message identifier composed of sender ID and per-sender sequence number. */
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
