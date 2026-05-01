package ch.trancee.meshlink.wire.messages

import ch.trancee.meshlink.wire.ReadBuffer
import ch.trancee.meshlink.wire.WriteBuffer

public object HandshakeMessageCodec {
    private const val ROUND_SIZE: Int = 1

    public fun encode(message: HandshakeMessage): ByteArray {
        val writeBuffer = WriteBuffer(initialCapacity = ROUND_SIZE + message.payload.size)
        writeBuffer.writeByte(value = message.round.code.toByte())
        writeBuffer.writeBytes(value = message.payload)
        return writeBuffer.toByteArray()
    }

    public fun decode(payload: ByteArray): HandshakeMessage {
        if (payload.size < ROUND_SIZE) {
            throw IllegalArgumentException("HANDSHAKE payload must contain at least the round byte.")
        }

        val readBuffer = ReadBuffer(source = payload)
        val round: HandshakeRound = HandshakeRound.fromCode(code = readBuffer.readByte().toUByte())
        val messagePayload: ByteArray = readBuffer.readBytes(length = readBuffer.remaining)

        return HandshakeMessage(
            round = round,
            payload = messagePayload,
        )
    }
}
