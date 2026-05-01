package ch.trancee.meshlink.wire.messages

import ch.trancee.meshlink.wire.WireMessage

/** Flooded multi-hop broadcast payload. */
public data class BroadcastMessage(
  public val originPeerId: ByteArray,
  public val sequenceNumber: Int,
  public val maxHops: UByte,
  public val payload: ByteArray,
) : WireMessage
