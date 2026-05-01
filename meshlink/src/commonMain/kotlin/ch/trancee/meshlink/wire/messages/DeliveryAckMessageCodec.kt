package ch.trancee.meshlink.wire.messages

import ch.trancee.meshlink.wire.ReadBuffer
import ch.trancee.meshlink.wire.WriteBuffer

/** Codec for [DeliveryAckMessage]. */
public object DeliveryAckMessageCodec {
  private const val PAYLOAD_SIZE: Int = Long.SIZE_BYTES

  public fun encode(message: DeliveryAckMessage): ByteArray {
    val writeBuffer = WriteBuffer(initialCapacity = PAYLOAD_SIZE)
    writeBuffer.writeLong(value = message.messageId)
    return writeBuffer.toByteArray()
  }

  public fun decode(payload: ByteArray): DeliveryAckMessage {
    if (payload.size != PAYLOAD_SIZE) {
      throw IllegalArgumentException("DELIVERY_ACK payload must be exactly $PAYLOAD_SIZE bytes.")
    }

    val readBuffer = ReadBuffer(source = payload)
    return DeliveryAckMessage(messageId = readBuffer.readLong())
  }
}
