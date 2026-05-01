package ch.trancee.meshlink.wire.messages

import ch.trancee.meshlink.wire.ReadBuffer
import ch.trancee.meshlink.wire.WriteBuffer

public object RotationAnnouncementMessageCodec {
    public const val PUBLIC_KEY_SIZE: Int = 32
    public const val SIGNATURE_SIZE: Int = 64
    private const val PAYLOAD_SIZE: Int = PUBLIC_KEY_SIZE + PUBLIC_KEY_SIZE + SIGNATURE_SIZE

    public fun encode(message: RotationAnnouncementMessage): ByteArray {
        if (message.previousPublicKey.size != PUBLIC_KEY_SIZE) {
            throw IllegalArgumentException("Rotation announcement previousPublicKey must be exactly $PUBLIC_KEY_SIZE bytes.")
        }
        if (message.nextPublicKey.size != PUBLIC_KEY_SIZE) {
            throw IllegalArgumentException("Rotation announcement nextPublicKey must be exactly $PUBLIC_KEY_SIZE bytes.")
        }
        if (message.signature.size != SIGNATURE_SIZE) {
            throw IllegalArgumentException("Rotation announcement signature must be exactly $SIGNATURE_SIZE bytes.")
        }

        val writeBuffer = WriteBuffer(initialCapacity = PAYLOAD_SIZE)
        writeBuffer.writeBytes(value = message.previousPublicKey)
        writeBuffer.writeBytes(value = message.nextPublicKey)
        writeBuffer.writeBytes(value = message.signature)
        return writeBuffer.toByteArray()
    }

    public fun decode(payload: ByteArray): RotationAnnouncementMessage {
        if (payload.size != PAYLOAD_SIZE) {
            throw IllegalArgumentException("ROTATION_ANNOUNCEMENT payload must be exactly $PAYLOAD_SIZE bytes.")
        }

        val readBuffer = ReadBuffer(source = payload)
        val previousPublicKey: ByteArray = readBuffer.readBytes(length = PUBLIC_KEY_SIZE)
        val nextPublicKey: ByteArray = readBuffer.readBytes(length = PUBLIC_KEY_SIZE)
        val signature: ByteArray = readBuffer.readBytes(length = SIGNATURE_SIZE)

        return RotationAnnouncementMessage(
            previousPublicKey = previousPublicKey,
            nextPublicKey = nextPublicKey,
            signature = signature,
        )
    }
}
