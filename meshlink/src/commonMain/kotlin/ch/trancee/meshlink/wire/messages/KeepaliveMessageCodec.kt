package ch.trancee.meshlink.wire.messages

public object KeepaliveMessageCodec {
    public fun encode(message: KeepaliveMessage = KeepaliveMessage): ByteArray {
        return ByteArray(size = 0)
    }

    public fun decode(payload: ByteArray): KeepaliveMessage {
        if (payload.isNotEmpty()) {
            throw IllegalArgumentException("KEEPALIVE payload must be empty.")
        }
        return KeepaliveMessage
    }
}
