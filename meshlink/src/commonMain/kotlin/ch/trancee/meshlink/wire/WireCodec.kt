package ch.trancee.meshlink.wire

import ch.trancee.meshlink.wire.messages.HandshakeMessage
import ch.trancee.meshlink.wire.messages.HandshakeMessageCodec
import ch.trancee.meshlink.wire.messages.HelloMessage
import ch.trancee.meshlink.wire.messages.HelloMessageCodec
import ch.trancee.meshlink.wire.messages.KeepaliveMessage
import ch.trancee.meshlink.wire.messages.KeepaliveMessageCodec
import ch.trancee.meshlink.wire.messages.RoutedMessage
import ch.trancee.meshlink.wire.messages.RoutedMessageCodec

public object WireCodec {
    private const val HEADER_SIZE: Int = 1 + Int.SIZE_BYTES

    public fun encode(message: WireMessage): ByteArray {
        val frameType: MessageType
        val payload: ByteArray

        when (message) {
            is HelloMessage -> {
                frameType = MessageType.HELLO
                payload = HelloMessageCodec.encode(message = message)
            }
            is HandshakeMessage -> {
                frameType = MessageType.HANDSHAKE
                payload = HandshakeMessageCodec.encode(message = message)
            }
            is KeepaliveMessage -> {
                frameType = MessageType.KEEPALIVE
                payload = KeepaliveMessageCodec.encode(message = message)
            }
            is RoutedMessage -> {
                frameType = MessageType.ROUTED_MESSAGE
                payload = RoutedMessageCodec.encode(message = message)
            }
            else -> throw UnsupportedOperationException(
                "WireCodec does not yet support encoding ${message::class.simpleName} messages.",
            )
        }

        val writeBuffer = WriteBuffer(initialCapacity = HEADER_SIZE + payload.size)
        writeBuffer.writeByte(value = frameType.code.toByte())
        writeBuffer.writeInt(value = payload.size)
        writeBuffer.writeBytes(value = payload)
        return writeBuffer.toByteArray()
    }

    public fun decode(encoded: ByteArray): WireMessage {
        if (encoded.size < HEADER_SIZE) {
            throw IllegalArgumentException("Encoded frame is shorter than the 5-byte header.")
        }

        val readBuffer = ReadBuffer(source = encoded)
        val type: MessageType = MessageType.fromCode(code = readBuffer.readByte().toUByte())
        val payloadSize: Int = readBuffer.readInt()
        if (payloadSize < 0) {
            throw IllegalArgumentException("Encoded frame declares a negative payload length.")
        }

        val expectedSize: Int = HEADER_SIZE + payloadSize
        if (encoded.size != expectedSize) {
            throw IllegalArgumentException(
                "Encoded frame length mismatch: expected $expectedSize bytes but received ${encoded.size}.",
            )
        }

        val payload: ByteArray = readBuffer.readBytes(length = payloadSize)
        return when (type) {
            MessageType.HELLO -> HelloMessageCodec.decode(payload = payload)
            MessageType.HANDSHAKE -> HandshakeMessageCodec.decode(payload = payload)
            MessageType.KEEPALIVE -> KeepaliveMessageCodec.decode(payload = payload)
            MessageType.ROUTED_MESSAGE -> RoutedMessageCodec.decode(payload = payload)
            else -> throw UnsupportedOperationException(
                "WireCodec does not yet support decoding ${type.name} messages.",
            )
        }
    }
}
