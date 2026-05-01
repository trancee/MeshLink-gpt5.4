package ch.trancee.meshlink.wire.messages

import ch.trancee.meshlink.wire.WireMessage

public data class ChunkAckMessage(
  public val transferId: Long,
  public val highestContiguousChunkIndex: Int,
  public val selectiveAckBitmap: ByteArray,
) : WireMessage
