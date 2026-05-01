package ch.trancee.meshlink.wire.messages

import ch.trancee.meshlink.wire.ReadBuffer
import ch.trancee.meshlink.wire.WriteBuffer

public object ChunkMessageCodec {
    private const val HEADER_SIZE: Int = Long.SIZE_BYTES + Int.SIZE_BYTES

    public fun encode(message: ChunkMessage): ByteArray {
        val writeBuffer = WriteBuffer(initialCapacity = HEADER_SIZE + message.payload.size)
        writeBuffer.writeLong(value = message.transferId)
        writeBuffer.writeInt(value = message.chunkIndex)
        writeBuffer.writeBytes(value = message.payload)
        return writeBuffer.toByteArray()
    }

    public fun decode(payload: ByteArray): ChunkMessage {
        if (payload.size < HEADER_SIZE) {
            throw IllegalArgumentException("CHUNK payload must contain transferId and chunkIndex.")
        }

        val readBuffer = ReadBuffer(source = payload)
        val transferId: Long = readBuffer.readLong()
        val chunkIndex: Int = readBuffer.readInt()
        val chunkPayload: ByteArray = readBuffer.readBytes(length = readBuffer.remaining)

        return ChunkMessage(
            transferId = transferId,
            chunkIndex = chunkIndex,
            payload = chunkPayload,
        )
    }
}
