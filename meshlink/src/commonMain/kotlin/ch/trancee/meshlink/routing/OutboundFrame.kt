package ch.trancee.meshlink.routing

import ch.trancee.meshlink.api.PeerIdHex

public data class OutboundFrame(
  public val nextHopPeerId: PeerIdHex,
  public val hopCount: Int,
  public val payload: ByteArray,
) {
  init {
    require(hopCount >= 0) { "OutboundFrame hopCount must be greater than or equal to 0." }
  }
}
