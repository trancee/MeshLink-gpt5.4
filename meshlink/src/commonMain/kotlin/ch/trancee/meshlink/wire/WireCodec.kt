package ch.trancee.meshlink.wire

import ch.trancee.meshlink.wire.messages.BroadcastMessage
import ch.trancee.meshlink.wire.messages.BroadcastMessageCodec
import ch.trancee.meshlink.wire.messages.ChunkAckMessage
import ch.trancee.meshlink.wire.messages.ChunkAckMessageCodec
import ch.trancee.meshlink.wire.messages.ChunkMessage
import ch.trancee.meshlink.wire.messages.ChunkMessageCodec
import ch.trancee.meshlink.wire.messages.DeliveryAckMessage
import ch.trancee.meshlink.wire.messages.DeliveryAckMessageCodec
import ch.trancee.meshlink.wire.messages.HandshakeMessage
import ch.trancee.meshlink.wire.messages.HandshakeMessageCodec
import ch.trancee.meshlink.wire.messages.HelloMessage
import ch.trancee.meshlink.wire.messages.HelloMessageCodec
import ch.trancee.meshlink.wire.messages.KeepaliveMessage
import ch.trancee.meshlink.wire.messages.KeepaliveMessageCodec
import ch.trancee.meshlink.wire.messages.NackMessage
import ch.trancee.meshlink.wire.messages.NackMessageCodec
import ch.trancee.meshlink.wire.messages.ResumeRequestMessage
import ch.trancee.meshlink.wire.messages.ResumeRequestMessageCodec
import ch.trancee.meshlink.wire.messages.RotationAnnouncementMessage
import ch.trancee.meshlink.wire.messages.RotationAnnouncementMessageCodec
import ch.trancee.meshlink.wire.messages.RoutedMessage
import ch.trancee.meshlink.wire.messages.RoutedMessageCodec
import ch.trancee.meshlink.wire.messages.UpdateMessage
import ch.trancee.meshlink.wire.messages.UpdateMessageCodec

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
            is UpdateMessage -> {
                frameType = MessageType.UPDATE
                payload = UpdateMessageCodec.encode(message = message)
            }
            is DeliveryAckMessage -> {
                frameType = MessageType.DELIVERY_ACK
                payload = DeliveryAckMessageCodec.encode(message = message)
            }
            is NackMessage -> {
                frameType = MessageType.NACK
                payload = NackMessageCodec.encode(message = message)
            }
            is ResumeRequestMessage -> {
                frameType = MessageType.RESUME_REQUEST
                payload = ResumeRequestMessageCodec.encode(message = message)
            }
            is BroadcastMessage -> {
                frameType = MessageType.BROADCAST
                payload = BroadcastMessageCodec.encode(message = message)
            }
            is ChunkMessage -> {
                frameType = MessageType.CHUNK
                payload = ChunkMessageCodec.encode(message = message)
            }
            is ChunkAckMessage -> {
                frameType = MessageType.CHUNK_ACK
                payload = ChunkAckMessageCodec.encode(message = message)
            }
            is RotationAnnouncementMessage -> {
                frameType = MessageType.ROTATION_ANNOUNCEMENT
                payload = RotationAnnouncementMessageCodec.encode(message = message)
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
            MessageType.UPDATE -> UpdateMessageCodec.decode(payload = payload)
            MessageType.DELIVERY_ACK -> DeliveryAckMessageCodec.decode(payload = payload)
            MessageType.NACK -> NackMessageCodec.decode(payload = payload)
            MessageType.RESUME_REQUEST -> ResumeRequestMessageCodec.decode(payload = payload)
            MessageType.BROADCAST -> BroadcastMessageCodec.decode(payload = payload)
            MessageType.CHUNK -> ChunkMessageCodec.decode(payload = payload)
            MessageType.CHUNK_ACK -> ChunkAckMessageCodec.decode(payload = payload)
            MessageType.ROTATION_ANNOUNCEMENT -> RotationAnnouncementMessageCodec.decode(payload = payload)
        }
    }
}
