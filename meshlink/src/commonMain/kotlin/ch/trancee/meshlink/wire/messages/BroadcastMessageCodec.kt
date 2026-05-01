package ch.trancee.meshlink.wire.messages

import ch.trancee.meshlink.wire.ReadBuffer
import ch.trancee.meshlink.wire.WriteBuffer

public object BroadcastMessageCodec {
  public const val ORIGIN_PEER_ID_SIZE: Int = HelloMessageCodec.PEER_ID_SIZE
  private const val HEADER_SIZE: Int = ORIGIN_PEER_ID_SIZE + Int.SIZE_BYTES + 1

  public fun encode(message: BroadcastMessage): ByteArray {
    if (message.originPeerId.size != ORIGIN_PEER_ID_SIZE) {
      throw IllegalArgumentException(
        "Broadcast originPeerId must be exactly $ORIGIN_PEER_ID_SIZE bytes."
      )
    }

    val writeBuffer = WriteBuffer(initialCapacity = HEADER_SIZE + message.payload.size)
    writeBuffer.writeBytes(value = message.originPeerId)
    writeBuffer.writeInt(value = message.sequenceNumber)
    writeBuffer.writeByte(value = message.maxHops.toByte())
    writeBuffer.writeBytes(value = message.payload)
    return writeBuffer.toByteArray()
  }

  public fun decode(payload: ByteArray): BroadcastMessage {
    if (payload.size < HEADER_SIZE) {
      throw IllegalArgumentException(
        "BROADCAST payload must contain originPeerId, sequenceNumber, and maxHops."
      )
    }

    val readBuffer = ReadBuffer(source = payload)
    val originPeerId: ByteArray = readBuffer.readBytes(length = ORIGIN_PEER_ID_SIZE)
    val sequenceNumber: Int = readBuffer.readInt()
    val maxHops: UByte = readBuffer.readByte().toUByte()
    val messagePayload: ByteArray = readBuffer.readBytes(length = readBuffer.remaining)

    return BroadcastMessage(
      originPeerId = originPeerId,
      sequenceNumber = sequenceNumber,
      maxHops = maxHops,
      payload = messagePayload,
    )
  }
}
