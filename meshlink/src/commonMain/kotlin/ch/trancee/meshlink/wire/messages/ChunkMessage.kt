package ch.trancee.meshlink.wire.messages

import ch.trancee.meshlink.wire.WireMessage

/** Transfer data chunk. */
public data class ChunkMessage(
  public val transferId: Long,
  public val chunkIndex: Int,
  public val payload: ByteArray,
) : WireMessage
