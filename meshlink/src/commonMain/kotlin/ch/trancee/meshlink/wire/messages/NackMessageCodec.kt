package ch.trancee.meshlink.wire.messages

import ch.trancee.meshlink.wire.ReadBuffer
import ch.trancee.meshlink.wire.WriteBuffer

public object NackMessageCodec {
  private const val PAYLOAD_SIZE: Int = Long.SIZE_BYTES + Int.SIZE_BYTES

  public fun encode(message: NackMessage): ByteArray {
    val writeBuffer = WriteBuffer(initialCapacity = PAYLOAD_SIZE)
    writeBuffer.writeLong(value = message.messageId)
    writeBuffer.writeInt(value = message.reasonCode)
    return writeBuffer.toByteArray()
  }

  public fun decode(payload: ByteArray): NackMessage {
    if (payload.size != PAYLOAD_SIZE) {
      throw IllegalArgumentException("NACK payload must be exactly $PAYLOAD_SIZE bytes.")
    }

    val readBuffer = ReadBuffer(source = payload)
    return NackMessage(messageId = readBuffer.readLong(), reasonCode = readBuffer.readInt())
  }
}
