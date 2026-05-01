package ch.trancee.meshlink.wire.messages

import ch.trancee.meshlink.wire.WireMessage

/** Selective acknowledgement for transfer chunks. */
public data class ChunkAckMessage(
  public val transferId: Long,
  public val highestContiguousChunkIndex: Int,
  public val selectiveAckBitmap: ByteArray,
) : WireMessage
