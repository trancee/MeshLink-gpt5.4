package ch.trancee.meshlink.wire.messages

import ch.trancee.meshlink.wire.ReadBuffer
import ch.trancee.meshlink.wire.WriteBuffer

/** Codec for [ResumeRequestMessage]. */
public object ResumeRequestMessageCodec {
  private const val PAYLOAD_SIZE: Int = Long.SIZE_BYTES + Long.SIZE_BYTES

  public fun encode(message: ResumeRequestMessage): ByteArray {
    val writeBuffer = WriteBuffer(initialCapacity = PAYLOAD_SIZE)
    writeBuffer.writeLong(value = message.transferId)
    writeBuffer.writeLong(value = message.resumeOffset)
    return writeBuffer.toByteArray()
  }

  public fun decode(payload: ByteArray): ResumeRequestMessage {
    if (payload.size != PAYLOAD_SIZE) {
      throw IllegalArgumentException("RESUME_REQUEST payload must be exactly $PAYLOAD_SIZE bytes.")
    }

    val readBuffer = ReadBuffer(source = payload)
    return ResumeRequestMessage(
      transferId = readBuffer.readLong(),
      resumeOffset = readBuffer.readLong(),
    )
  }
}
