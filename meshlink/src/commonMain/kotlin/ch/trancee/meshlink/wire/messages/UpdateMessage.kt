package ch.trancee.meshlink.wire.messages

import ch.trancee.meshlink.wire.WireMessage

public data class UpdateMessage(
  public val destinationPeerId: ByteArray,
  public val metric: Int,
  public val seqno: Int,
) : WireMessage
