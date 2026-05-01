package ch.trancee.meshlink.wire

import ch.trancee.meshlink.wire.messages.HandshakeMessage
import ch.trancee.meshlink.wire.messages.HandshakeRound
import ch.trancee.meshlink.wire.messages.HelloMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

public class InboundValidatorTest {
    @Test
    public fun validate_returnsValidForWellFormedHelloFrame(): Unit {
        // Arrange
        val validator = InboundValidator()
        val encoded: ByteArray = WireCodec.encode(
            message = HelloMessage(
                peerId = byteArrayOf(0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C),
                appIdHash = 0x01020304,
            ),
        )

        // Act
        val actual: ValidationResult = validator.validate(encoded = encoded)

        // Assert
        assertEquals(
            expected = ValidationResult.Valid,
            actual = actual,
            message = "InboundValidator should accept well-formed HELLO frames within the configured bounds",
        )
    }

    @Test
    public fun validate_returnsValidForWellFormedHandshakeFrame(): Unit {
        // Arrange
        val validator = InboundValidator()
        val encoded: ByteArray = WireCodec.encode(
            message = HandshakeMessage(
                round = HandshakeRound.TWO,
                payload = byteArrayOf(0x21, 0x22),
            ),
        )

        // Act
        val actual: ValidationResult = validator.validate(encoded = encoded)

        // Assert
        assertEquals(
            expected = ValidationResult.Valid,
            actual = actual,
            message = "InboundValidator should accept well-formed HANDSHAKE frames that include a round byte",
        )
    }

    @Test
    public fun validate_rejectsFramesShorterThanHeader(): Unit {
        // Arrange
        val validator = InboundValidator()
        val encoded: ByteArray = byteArrayOf(MessageType.HELLO.code.toByte(), 0x00, 0x00, 0x00)

        // Act
        val actual: ValidationResult.Invalid = assertIs<ValidationResult.Invalid>(validator.validate(encoded = encoded))

        // Assert
        assertEquals(
            expected = ValidationFailureCode.TRUNCATED_HEADER,
            actual = actual.code,
            message = "InboundValidator should classify header truncation failures explicitly",
        )
        assertEquals(
            expected = "Encoded frame is shorter than the 5-byte header.",
            actual = actual.reason,
            message = "InboundValidator should explain header truncation failures clearly",
        )
    }

    @Test
    public fun validate_rejectsUnknownMessageTypeCodes(): Unit {
        // Arrange
        val validator = InboundValidator()
        val encoded: ByteArray = byteArrayOf(0x7F, 0x00, 0x00, 0x00, 0x00)

        // Act
        val actual: ValidationResult.Invalid = assertIs<ValidationResult.Invalid>(validator.validate(encoded = encoded))

        // Assert
        assertEquals(
            expected = ValidationFailureCode.UNKNOWN_MESSAGE_TYPE,
            actual = actual.code,
            message = "InboundValidator should classify unknown message tags explicitly",
        )
        assertEquals(
            expected = "Encoded frame declares an unknown message type.",
            actual = actual.reason,
            message = "InboundValidator should explain unknown message tags clearly",
        )
    }

    @Test
    public fun validate_rejectsFramesWithNegativePayloadLength(): Unit {
        // Arrange
        val validator = InboundValidator()
        val encoded: ByteArray = byteArrayOf(
            MessageType.HELLO.code.toByte(),
            0xFF.toByte(),
            0xFF.toByte(),
            0xFF.toByte(),
            0xFF.toByte(),
        )

        // Act
        val actual: ValidationResult.Invalid = assertIs<ValidationResult.Invalid>(validator.validate(encoded = encoded))

        // Assert
        assertEquals(
            expected = ValidationFailureCode.NEGATIVE_PAYLOAD_LENGTH,
            actual = actual.code,
            message = "InboundValidator should reject frames whose payload length header decodes to a negative value",
        )
        assertEquals(
            expected = "Encoded frame declares a negative payload length.",
            actual = actual.reason,
            message = "InboundValidator should explain negative payload lengths clearly",
        )
    }

    @Test
    public fun validate_rejectsFramesWhoseDeclaredLengthDoesNotMatchActualBytes(): Unit {
        // Arrange
        val validator = InboundValidator()
        val encoded: ByteArray = byteArrayOf(
            MessageType.HELLO.code.toByte(),
            0x02,
            0x00,
            0x00,
            0x00,
            0x0A,
        )

        // Act
        val actual: ValidationResult.Invalid = assertIs<ValidationResult.Invalid>(validator.validate(encoded = encoded))

        // Assert
        assertEquals(
            expected = ValidationFailureCode.PAYLOAD_LENGTH_MISMATCH,
            actual = actual.code,
            message = "InboundValidator should reject frames whose declared payload length differs from the actual byte count",
        )
        assertEquals(
            expected = "Encoded frame length mismatch: expected 7 bytes but received 6.",
            actual = actual.reason,
            message = "InboundValidator should explain payload length mismatches clearly",
        )
    }

    @Test
    public fun validate_rejectsFramesWhosePayloadExceedsConfiguredMaximum(): Unit {
        // Arrange
        val validator = InboundValidator(maxPayloadSize = 3)
        val encoded: ByteArray = WireCodec.encode(
            message = HandshakeMessage(
                round = HandshakeRound.ONE,
                payload = byteArrayOf(0x31, 0x32, 0x33),
            ),
        )

        // Act
        val actual: ValidationResult.Invalid = assertIs<ValidationResult.Invalid>(validator.validate(encoded = encoded))

        // Assert
        assertEquals(
            expected = ValidationFailureCode.PAYLOAD_EXCEEDS_MAX_SIZE,
            actual = actual.code,
            message = "InboundValidator should reject payloads that exceed the configured maximum size",
        )
        assertEquals(
            expected = "Encoded frame payload exceeds the configured maximum size of 3 bytes.",
            actual = actual.reason,
            message = "InboundValidator should report the configured maximum payload size in its failure reason",
        )
    }

    @Test
    public fun validate_acceptsCurrentlyUnconstrainedMessageTypesWhenHeaderIsWellFormed(): Unit {
        // Arrange
        val validator = InboundValidator()
        val encoded: ByteArray = byteArrayOf(
            MessageType.UPDATE.code.toByte(),
            0x00,
            0x00,
            0x00,
            0x00,
        )

        // Act
        val actual: ValidationResult = validator.validate(encoded = encoded)

        // Assert
        assertEquals(
            expected = ValidationResult.Valid,
            actual = actual,
            message = "InboundValidator should allow currently unconstrained message types when their frame header is well formed",
        )
    }

    @Test
    public fun validate_rejectsHelloFramesWithUnexpectedPayloadLength(): Unit {
        // Arrange
        val validator = InboundValidator()
        val encoded: ByteArray = byteArrayOf(
            MessageType.HELLO.code.toByte(),
            0x01,
            0x00,
            0x00,
            0x00,
            0x7F,
        )

        // Act
        val actual: ValidationResult.Invalid = assertIs<ValidationResult.Invalid>(validator.validate(encoded = encoded))

        // Assert
        assertEquals(
            expected = ValidationFailureCode.HELLO_PAYLOAD_SIZE_INVALID,
            actual = actual.code,
            message = "InboundValidator should reject HELLO frames whose payload size is not exactly 16 bytes",
        )
        assertEquals(
            expected = "HELLO payload must be exactly 16 bytes.",
            actual = actual.reason,
            message = "InboundValidator should explain HELLO payload size violations clearly",
        )
    }

    @Test
    public fun validate_rejectsHandshakeFramesWithoutRoundByte(): Unit {
        // Arrange
        val validator = InboundValidator()
        val encoded: ByteArray = byteArrayOf(
            MessageType.HANDSHAKE.code.toByte(),
            0x00,
            0x00,
            0x00,
            0x00,
        )

        // Act
        val actual: ValidationResult.Invalid = assertIs<ValidationResult.Invalid>(validator.validate(encoded = encoded))

        // Assert
        assertEquals(
            expected = ValidationFailureCode.HANDSHAKE_PAYLOAD_TOO_SHORT,
            actual = actual.code,
            message = "InboundValidator should reject HANDSHAKE frames that do not contain the round byte",
        )
        assertEquals(
            expected = "HANDSHAKE payload must contain at least the round byte.",
            actual = actual.reason,
            message = "InboundValidator should explain HANDSHAKE payload truncation clearly",
        )
    }
}
