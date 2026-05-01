package ch.trancee.meshlink.wire.messages

import ch.trancee.meshlink.wire.ReadBuffer
import ch.trancee.meshlink.wire.WriteBuffer

public object HelloMessageCodec {
  public const val PEER_ID_SIZE: Int = 12
  private const val PAYLOAD_SIZE: Int = PEER_ID_SIZE + Int.SIZE_BYTES

  public fun encode(message: HelloMessage): ByteArray {
    if (message.peerId.size != PEER_ID_SIZE) {
      throw IllegalArgumentException("Hello peerId must be exactly $PEER_ID_SIZE bytes.")
    }

    val writeBuffer = WriteBuffer(initialCapacity = PAYLOAD_SIZE)
    writeBuffer.writeBytes(message.peerId)
    writeBuffer.writeInt(message.appIdHash)
    return writeBuffer.toByteArray()
  }

  public fun decode(payload: ByteArray): HelloMessage {
    if (payload.size != PAYLOAD_SIZE) {
      throw IllegalArgumentException("HELLO payload must be exactly $PAYLOAD_SIZE bytes.")
    }

    val readBuffer = ReadBuffer(source = payload)
    val peerId: ByteArray = readBuffer.readBytes(length = PEER_ID_SIZE)
    val appIdHash: Int = readBuffer.readInt()

    return HelloMessage(peerId = peerId, appIdHash = appIdHash)
  }
}
