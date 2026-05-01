package ch.trancee.meshlink.wire.messages

import ch.trancee.meshlink.wire.ReadBuffer
import ch.trancee.meshlink.wire.WriteBuffer

public object RoutedMessageCodec {
    private const val HEADER_SIZE: Int = 2

    public fun encode(message: RoutedMessage): ByteArray {
        val writeBuffer = WriteBuffer(initialCapacity = HEADER_SIZE + message.payload.size)
        writeBuffer.writeByte(value = message.hopCount.toByte())
        writeBuffer.writeByte(value = message.maxHops.toByte())
        writeBuffer.writeBytes(value = message.payload)
        return writeBuffer.toByteArray()
    }

    public fun decode(payload: ByteArray): RoutedMessage {
        if (payload.size < HEADER_SIZE) {
            throw IllegalArgumentException("ROUTED_MESSAGE payload must contain hopCount and maxHops bytes.")
        }

        val readBuffer = ReadBuffer(source = payload)
        val hopCount: UByte = readBuffer.readByte().toUByte()
        val maxHops: UByte = readBuffer.readByte().toUByte()
        val messagePayload: ByteArray = readBuffer.readBytes(length = readBuffer.remaining)

        return RoutedMessage(
            hopCount = hopCount,
            maxHops = maxHops,
            payload = messagePayload,
        )
    }
}
