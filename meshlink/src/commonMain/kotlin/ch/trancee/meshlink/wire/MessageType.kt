package ch.trancee.meshlink.wire

public enum class MessageType(public val code: UByte) {
    HELLO(0x01u),
    HANDSHAKE(0x02u),
    UPDATE(0x03u),
    CHUNK(0x04u),
    CHUNK_ACK(0x05u),
    DELIVERY_ACK(0x06u),
    NACK(0x07u),
    KEEPALIVE(0x08u),
    BROADCAST(0x09u),
    ROUTED_MESSAGE(0x0Au),
    RESUME_REQUEST(0x0Bu),
    ROTATION_ANNOUNCEMENT(0x0Cu),
    ;

    public companion object {
        public fun fromCode(code: UByte): MessageType {
            return entries.firstOrNull { entry -> entry.code == code }
                ?: throw IllegalArgumentException("Unknown message type code: 0x${code.toString(16).padStart(2, '0')}.")
        }
    }
}
