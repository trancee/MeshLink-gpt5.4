package ch.trancee.meshlink.wire.messages

import ch.trancee.meshlink.wire.ReadBuffer
import ch.trancee.meshlink.wire.WriteBuffer

/** Codec for [UpdateMessage]. */
public object UpdateMessageCodec {
  public const val DESTINATION_PEER_ID_SIZE: Int = HelloMessageCodec.PEER_ID_SIZE
  private const val PAYLOAD_SIZE: Int = DESTINATION_PEER_ID_SIZE + Int.SIZE_BYTES + Int.SIZE_BYTES

  public fun encode(message: UpdateMessage): ByteArray {
    if (message.destinationPeerId.size != DESTINATION_PEER_ID_SIZE) {
      throw IllegalArgumentException(
        "Update destinationPeerId must be exactly $DESTINATION_PEER_ID_SIZE bytes."
      )
    }

    val writeBuffer = WriteBuffer(initialCapacity = PAYLOAD_SIZE)
    writeBuffer.writeBytes(value = message.destinationPeerId)
    writeBuffer.writeInt(value = message.metric)
    writeBuffer.writeInt(value = message.seqno)
    return writeBuffer.toByteArray()
  }

  public fun decode(payload: ByteArray): UpdateMessage {
    if (payload.size != PAYLOAD_SIZE) {
      throw IllegalArgumentException("UPDATE payload must be exactly $PAYLOAD_SIZE bytes.")
    }

    val readBuffer = ReadBuffer(source = payload)
    val destinationPeerId: ByteArray = readBuffer.readBytes(length = DESTINATION_PEER_ID_SIZE)
    val metric: Int = readBuffer.readInt()
    val seqno: Int = readBuffer.readInt()

    return UpdateMessage(destinationPeerId = destinationPeerId, metric = metric, seqno = seqno)
  }
}
