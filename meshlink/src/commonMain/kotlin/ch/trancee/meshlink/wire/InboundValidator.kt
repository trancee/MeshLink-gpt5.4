package ch.trancee.meshlink.wire

import ch.trancee.meshlink.wire.messages.HandshakeMessageCodec
import ch.trancee.meshlink.wire.messages.HelloMessageCodec

public class InboundValidator(
    private val maxPayloadSize: Int = DEFAULT_MAX_PAYLOAD_SIZE,
) {
    public fun validate(encoded: ByteArray): ValidationResult {
        if (encoded.size < HEADER_SIZE) {
            return ValidationResult.Invalid(
                code = ValidationFailureCode.TRUNCATED_HEADER,
                reason = "Encoded frame is shorter than the 5-byte header.",
            )
        }

        val readBuffer = ReadBuffer(source = encoded)
        val typeCode: UByte = readBuffer.readByte().toUByte()
        val type: MessageType = try {
            MessageType.fromCode(code = typeCode)
        } catch (_: IllegalArgumentException) {
            return ValidationResult.Invalid(
                code = ValidationFailureCode.UNKNOWN_MESSAGE_TYPE,
                reason = "Encoded frame declares an unknown message type.",
            )
        }

        val payloadSize: Int = readBuffer.readInt()
        if (payloadSize < 0) {
            return ValidationResult.Invalid(
                code = ValidationFailureCode.NEGATIVE_PAYLOAD_LENGTH,
                reason = "Encoded frame declares a negative payload length.",
            )
        }
        if (payloadSize > maxPayloadSize) {
            return ValidationResult.Invalid(
                code = ValidationFailureCode.PAYLOAD_EXCEEDS_MAX_SIZE,
                reason = "Encoded frame payload exceeds the configured maximum size of $maxPayloadSize bytes.",
            )
        }

        val expectedSize: Int = HEADER_SIZE + payloadSize
        if (encoded.size != expectedSize) {
            return ValidationResult.Invalid(
                code = ValidationFailureCode.PAYLOAD_LENGTH_MISMATCH,
                reason = "Encoded frame length mismatch: expected $expectedSize bytes but received ${encoded.size}.",
            )
        }

        return when (type) {
            MessageType.HELLO -> validateHello(payloadSize = payloadSize)
            MessageType.HANDSHAKE -> validateHandshake(payloadSize = payloadSize)
            MessageType.ROUTED_MESSAGE -> validateRoutedMessage(encoded = encoded, payloadSize = payloadSize)
            else -> ValidationResult.Valid
        }
    }

    private fun validateHello(payloadSize: Int): ValidationResult {
        return if (payloadSize == HelloMessageCodec.PEER_ID_SIZE + Int.SIZE_BYTES) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(
                code = ValidationFailureCode.HELLO_PAYLOAD_SIZE_INVALID,
                reason = "HELLO payload must be exactly ${HelloMessageCodec.PEER_ID_SIZE + Int.SIZE_BYTES} bytes.",
            )
        }
    }

    private fun validateHandshake(payloadSize: Int): ValidationResult {
        return if (payloadSize >= 1) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(
                code = ValidationFailureCode.HANDSHAKE_PAYLOAD_TOO_SHORT,
                reason = "HANDSHAKE payload must contain at least the round byte.",
            )
        }
    }

    private fun validateRoutedMessage(encoded: ByteArray, payloadSize: Int): ValidationResult {
        if (payloadSize < ROUTED_HEADER_SIZE) {
            return ValidationResult.Invalid(
                code = ValidationFailureCode.ROUTED_MESSAGE_PAYLOAD_TOO_SHORT,
                reason = "ROUTED_MESSAGE payload must contain hopCount and maxHops bytes.",
            )
        }

        val payloadOffset: Int = HEADER_SIZE
        val hopCount: UByte = encoded[payloadOffset].toUByte()
        val maxHops: UByte = encoded[payloadOffset + 1].toUByte()

        return if (hopCount < maxHops) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(
                code = ValidationFailureCode.HOP_LIMIT_EXCEEDED,
                reason = "ROUTED_MESSAGE hopCount must stay below maxHops.",
            )
        }
    }

    public companion object {
        public const val DEFAULT_MAX_PAYLOAD_SIZE: Int = 512
        private const val HEADER_SIZE: Int = 1 + Int.SIZE_BYTES
        private const val ROUTED_HEADER_SIZE: Int = 2
    }
}
