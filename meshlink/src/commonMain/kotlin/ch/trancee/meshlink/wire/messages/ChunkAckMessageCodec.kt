package ch.trancee.meshlink.wire.messages

import ch.trancee.meshlink.wire.ReadBuffer
import ch.trancee.meshlink.wire.WriteBuffer

/** Codec for [ChunkAckMessage]. */
public object ChunkAckMessageCodec {
  private const val HEADER_SIZE: Int = Long.SIZE_BYTES + Int.SIZE_BYTES

  public fun encode(message: ChunkAckMessage): ByteArray {
    val writeBuffer = WriteBuffer(initialCapacity = HEADER_SIZE + message.selectiveAckBitmap.size)
    writeBuffer.writeLong(value = message.transferId)
    writeBuffer.writeInt(value = message.highestContiguousChunkIndex)
    writeBuffer.writeBytes(value = message.selectiveAckBitmap)
    return writeBuffer.toByteArray()
  }

  public fun decode(payload: ByteArray): ChunkAckMessage {
    if (payload.size < HEADER_SIZE) {
      throw IllegalArgumentException(
        "CHUNK_ACK payload must contain transferId and highestContiguousChunkIndex."
      )
    }

    val readBuffer = ReadBuffer(source = payload)
    val transferId: Long = readBuffer.readLong()
    val highestContiguousChunkIndex: Int = readBuffer.readInt()
    val selectiveAckBitmap: ByteArray = readBuffer.readBytes(length = readBuffer.remaining)

    return ChunkAckMessage(
      transferId = transferId,
      highestContiguousChunkIndex = highestContiguousChunkIndex,
      selectiveAckBitmap = selectiveAckBitmap,
    )
  }
}
