package ch.trancee.meshlink.wire

import ch.trancee.meshlink.wire.messages.BroadcastMessage
import ch.trancee.meshlink.wire.messages.ChunkAckMessage
import ch.trancee.meshlink.wire.messages.ChunkMessage
import ch.trancee.meshlink.wire.messages.DeliveryAckMessage
import ch.trancee.meshlink.wire.messages.HandshakeMessage
import ch.trancee.meshlink.wire.messages.HandshakeRound
import ch.trancee.meshlink.wire.messages.HelloMessage
import ch.trancee.meshlink.wire.messages.KeepaliveMessage
import ch.trancee.meshlink.wire.messages.NackMessage
import ch.trancee.meshlink.wire.messages.ResumeRequestMessage
import ch.trancee.meshlink.wire.messages.RotationAnnouncementMessage
import ch.trancee.meshlink.wire.messages.RoutedMessage
import ch.trancee.meshlink.wire.messages.UpdateMessage
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
    public fun validate_returnsValidForWellFormedUpdateFrame(): Unit {
        // Arrange
        val validator = InboundValidator()
        val encoded: ByteArray = WireCodec.encode(
            message = UpdateMessage(
                destinationPeerId = byteArrayOf(0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C),
                metric = 10,
                seqno = 5,
            ),
        )

        // Act
        val actual: ValidationResult = validator.validate(encoded = encoded)

        // Assert
        assertEquals(
            expected = ValidationResult.Valid,
            actual = actual,
            message = "InboundValidator should accept well-formed UPDATE frames with the expected fixed payload size",
        )
    }

    @Test
    public fun validate_rejectsUpdateFramesWithUnexpectedPayloadLength(): Unit {
        // Arrange
        val validator = InboundValidator()
        val encoded: ByteArray = byteArrayOf(
            MessageType.UPDATE.code.toByte(),
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
            expected = ValidationFailureCode.UPDATE_PAYLOAD_SIZE_INVALID,
            actual = actual.code,
            message = "InboundValidator should reject UPDATE frames whose payload size is not exactly 20 bytes",
        )
        assertEquals(
            expected = "UPDATE payload must be exactly 20 bytes.",
            actual = actual.reason,
            message = "InboundValidator should explain UPDATE payload size violations clearly",
        )
    }

    @Test
    public fun validate_returnsValidForRoutedMessagesBelowHopLimit(): Unit {
        // Arrange
        val validator = InboundValidator()
        val encoded: ByteArray = WireCodec.encode(
            message = RoutedMessage(
                hopCount = 1u,
                maxHops = 3u,
                payload = byteArrayOf(0x41),
            ),
        )

        // Act
        val actual: ValidationResult = validator.validate(encoded = encoded)

        // Assert
        assertEquals(
            expected = ValidationResult.Valid,
            actual = actual,
            message = "InboundValidator should accept routed messages whose hopCount remains below maxHops",
        )
    }

    @Test
    public fun validate_rejectsRoutedMessagesWithoutHopHeader(): Unit {
        // Arrange
        val validator = InboundValidator()
        val encoded: ByteArray = byteArrayOf(
            MessageType.ROUTED_MESSAGE.code.toByte(),
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
            expected = ValidationFailureCode.ROUTED_MESSAGE_PAYLOAD_TOO_SHORT,
            actual = actual.code,
            message = "InboundValidator should reject routed messages that do not contain both hop header bytes",
        )
        assertEquals(
            expected = "ROUTED_MESSAGE payload must contain hopCount and maxHops bytes.",
            actual = actual.reason,
            message = "InboundValidator should explain routed-message header truncation clearly",
        )
    }

    @Test
    public fun validate_returnsValidForWellFormedChunkFrame(): Unit {
        // Arrange
        val validator = InboundValidator()
        val encoded: ByteArray = WireCodec.encode(
            message = ChunkMessage(
                transferId = 0x0102030405060708,
                chunkIndex = 1,
                payload = byteArrayOf(0x11),
            ),
        )

        // Act
        val actual: ValidationResult = validator.validate(encoded = encoded)

        // Assert
        assertEquals(
            expected = ValidationResult.Valid,
            actual = actual,
            message = "InboundValidator should accept CHUNK frames that include transferId and chunkIndex",
        )
    }

    @Test
    public fun validate_rejectsChunkFramesWithoutHeader(): Unit {
        // Arrange
        val validator = InboundValidator()
        val encoded: ByteArray = byteArrayOf(
            MessageType.CHUNK.code.toByte(),
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
            expected = ValidationFailureCode.CHUNK_PAYLOAD_TOO_SHORT,
            actual = actual.code,
            message = "InboundValidator should reject CHUNK frames whose header is truncated",
        )
        assertEquals(
            expected = "CHUNK payload must contain transferId and chunkIndex.",
            actual = actual.reason,
            message = "InboundValidator should explain CHUNK header truncation clearly",
        )
    }

    @Test
    public fun validate_returnsValidForWellFormedChunkAckFrame(): Unit {
        // Arrange
        val validator = InboundValidator()
        val encoded: ByteArray = WireCodec.encode(
            message = ChunkAckMessage(
                transferId = 0x1112131415161718,
                highestContiguousChunkIndex = 2,
                selectiveAckBitmap = byteArrayOf(0x01),
            ),
        )

        // Act
        val actual: ValidationResult = validator.validate(encoded = encoded)

        // Assert
        assertEquals(
            expected = ValidationResult.Valid,
            actual = actual,
            message = "InboundValidator should accept CHUNK_ACK frames that include transferId and highestContiguousChunkIndex",
        )
    }

    @Test
    public fun validate_rejectsChunkAckFramesWithoutHeader(): Unit {
        // Arrange
        val validator = InboundValidator()
        val encoded: ByteArray = byteArrayOf(
            MessageType.CHUNK_ACK.code.toByte(),
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
            expected = ValidationFailureCode.CHUNK_ACK_PAYLOAD_TOO_SHORT,
            actual = actual.code,
            message = "InboundValidator should reject CHUNK_ACK frames whose header is truncated",
        )
        assertEquals(
            expected = "CHUNK_ACK payload must contain transferId and highestContiguousChunkIndex.",
            actual = actual.reason,
            message = "InboundValidator should explain CHUNK_ACK header truncation clearly",
        )
    }

    @Test
    public fun validate_rejectsRoutedMessagesAtHopLimit(): Unit {
        // Arrange
        val validator = InboundValidator()
        val encoded: ByteArray = WireCodec.encode(
            message = RoutedMessage(
                hopCount = 3u,
                maxHops = 3u,
                payload = byteArrayOf(0x51),
            ),
        )

        // Act
        val actual: ValidationResult.Invalid = assertIs<ValidationResult.Invalid>(validator.validate(encoded = encoded))

        // Assert
        assertEquals(
            expected = ValidationFailureCode.HOP_LIMIT_EXCEEDED,
            actual = actual.code,
            message = "InboundValidator should reject routed messages once the hop limit is reached",
        )
        assertEquals(
            expected = "ROUTED_MESSAGE hopCount must stay below maxHops.",
            actual = actual.reason,
            message = "InboundValidator should explain hop-limit violations clearly",
        )
    }

    @Test
    public fun validate_returnsValidForWellFormedDeliveryAckFrame(): Unit {
        // Arrange
        val validator = InboundValidator()
        val encoded: ByteArray = WireCodec.encode(message = DeliveryAckMessage(messageId = 0x2122232425262728))

        // Act
        val actual: ValidationResult = validator.validate(encoded = encoded)

        // Assert
        assertEquals(
            expected = ValidationResult.Valid,
            actual = actual,
            message = "InboundValidator should accept DELIVERY_ACK frames with the expected fixed payload size",
        )
    }

    @Test
    public fun validate_rejectsDeliveryAckFramesWithUnexpectedPayloadLength(): Unit {
        // Arrange
        val validator = InboundValidator()
        val encoded: ByteArray = byteArrayOf(
            MessageType.DELIVERY_ACK.code.toByte(),
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
            expected = ValidationFailureCode.DELIVERY_ACK_PAYLOAD_SIZE_INVALID,
            actual = actual.code,
            message = "InboundValidator should reject DELIVERY_ACK frames with malformed fixed payload sizes",
        )
        assertEquals(
            expected = "DELIVERY_ACK payload must be exactly 8 bytes.",
            actual = actual.reason,
            message = "InboundValidator should explain DELIVERY_ACK payload size violations clearly",
        )
    }

    @Test
    public fun validate_returnsValidForWellFormedNackFrame(): Unit {
        // Arrange
        val validator = InboundValidator()
        val encoded: ByteArray = WireCodec.encode(
            message = NackMessage(
                messageId = 0x3132333435363738,
                reasonCode = 404,
            ),
        )

        // Act
        val actual: ValidationResult = validator.validate(encoded = encoded)

        // Assert
        assertEquals(
            expected = ValidationResult.Valid,
            actual = actual,
            message = "InboundValidator should accept NACK frames with the expected fixed payload size",
        )
    }

    @Test
    public fun validate_rejectsNackFramesWithUnexpectedPayloadLength(): Unit {
        // Arrange
        val validator = InboundValidator()
        val encoded: ByteArray = byteArrayOf(
            MessageType.NACK.code.toByte(),
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
            expected = ValidationFailureCode.NACK_PAYLOAD_SIZE_INVALID,
            actual = actual.code,
            message = "InboundValidator should reject NACK frames with malformed fixed payload sizes",
        )
        assertEquals(
            expected = "NACK payload must be exactly 12 bytes.",
            actual = actual.reason,
            message = "InboundValidator should explain NACK payload size violations clearly",
        )
    }

    @Test
    public fun validate_returnsValidForWellFormedKeepaliveFrame(): Unit {
        // Arrange
        val validator = InboundValidator()
        val encoded: ByteArray = WireCodec.encode(message = KeepaliveMessage)

        // Act
        val actual: ValidationResult = validator.validate(encoded = encoded)

        // Assert
        assertEquals(
            expected = ValidationResult.Valid,
            actual = actual,
            message = "InboundValidator should accept KEEPALIVE frames with empty payloads",
        )
    }

    @Test
    public fun validate_rejectsKeepaliveFramesWithUnexpectedPayloadBytes(): Unit {
        // Arrange
        val validator = InboundValidator()
        val encoded: ByteArray = byteArrayOf(
            MessageType.KEEPALIVE.code.toByte(),
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
            expected = ValidationFailureCode.KEEPALIVE_PAYLOAD_NOT_EMPTY,
            actual = actual.code,
            message = "InboundValidator should reject KEEPALIVE frames that contain payload bytes",
        )
        assertEquals(
            expected = "KEEPALIVE payload must be empty.",
            actual = actual.reason,
            message = "InboundValidator should explain KEEPALIVE payload violations clearly",
        )
    }

    @Test
    public fun validate_returnsValidForWellFormedBroadcastFrame(): Unit {
        // Arrange
        val validator = InboundValidator()
        val encoded: ByteArray = WireCodec.encode(
            message = BroadcastMessage(
                originPeerId = byteArrayOf(0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4A, 0x4B, 0x4C),
                sequenceNumber = 3,
                maxHops = 2u,
                payload = byteArrayOf(0x51),
            ),
        )

        // Act
        val actual: ValidationResult = validator.validate(encoded = encoded)

        // Assert
        assertEquals(
            expected = ValidationResult.Valid,
            actual = actual,
            message = "InboundValidator should accept BROADCAST frames whose header is complete and maxHops is positive",
        )
    }

    @Test
    public fun validate_rejectsBroadcastFramesWithoutCompleteHeader(): Unit {
        // Arrange
        val validator = InboundValidator()
        val encoded: ByteArray = byteArrayOf(
            MessageType.BROADCAST.code.toByte(),
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
            expected = ValidationFailureCode.BROADCAST_PAYLOAD_TOO_SHORT,
            actual = actual.code,
            message = "InboundValidator should reject BROADCAST frames whose header is truncated",
        )
        assertEquals(
            expected = "BROADCAST payload must contain originPeerId, sequenceNumber, and maxHops.",
            actual = actual.reason,
            message = "InboundValidator should explain BROADCAST header truncation clearly",
        )
    }

    @Test
    public fun validate_rejectsBroadcastFramesWithZeroMaxHops(): Unit {
        // Arrange
        val validator = InboundValidator()
        val encoded: ByteArray = WireCodec.encode(
            message = BroadcastMessage(
                originPeerId = byteArrayOf(0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5A, 0x5B, 0x5C),
                sequenceNumber = 4,
                maxHops = 0u,
                payload = byteArrayOf(0x61),
            ),
        )

        // Act
        val actual: ValidationResult.Invalid = assertIs<ValidationResult.Invalid>(validator.validate(encoded = encoded))

        // Assert
        assertEquals(
            expected = ValidationFailureCode.BROADCAST_MAX_HOPS_INVALID,
            actual = actual.code,
            message = "InboundValidator should reject BROADCAST frames whose maxHops is zero",
        )
        assertEquals(
            expected = "BROADCAST maxHops must be greater than zero.",
            actual = actual.reason,
            message = "InboundValidator should explain BROADCAST maxHops violations clearly",
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
    public fun validate_returnsValidForWellFormedResumeRequestFrame(): Unit {
        // Arrange
        val validator = InboundValidator()
        val encoded: ByteArray = WireCodec.encode(
            message = ResumeRequestMessage(
                transferId = 0x6162636465666768,
                resumeOffset = 0x7172737475767778,
            ),
        )

        // Act
        val actual: ValidationResult = validator.validate(encoded = encoded)

        // Assert
        assertEquals(
            expected = ValidationResult.Valid,
            actual = actual,
            message = "InboundValidator should accept RESUME_REQUEST frames with the expected fixed payload size",
        )
    }

    @Test
    public fun validate_rejectsResumeRequestFramesWithUnexpectedPayloadLength(): Unit {
        // Arrange
        val validator = InboundValidator()
        val encoded: ByteArray = byteArrayOf(
            MessageType.RESUME_REQUEST.code.toByte(),
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
            expected = ValidationFailureCode.RESUME_REQUEST_PAYLOAD_SIZE_INVALID,
            actual = actual.code,
            message = "InboundValidator should reject RESUME_REQUEST frames with malformed fixed payload sizes",
        )
        assertEquals(
            expected = "RESUME_REQUEST payload must be exactly 16 bytes.",
            actual = actual.reason,
            message = "InboundValidator should explain RESUME_REQUEST payload size violations clearly",
        )
    }

    @Test
    public fun validate_returnsValidForWellFormedRotationAnnouncementFrame(): Unit {
        // Arrange
        val validator = InboundValidator()
        val encoded: ByteArray = WireCodec.encode(
            message = RotationAnnouncementMessage(
                previousPublicKey = ByteArray(size = 32) { index -> (index + 1).toByte() },
                nextPublicKey = ByteArray(size = 32) { index -> (index + 33).toByte() },
                signature = ByteArray(size = 64) { index -> (index + 65).toByte() },
            ),
        )

        // Act
        val actual: ValidationResult = validator.validate(encoded = encoded)

        // Assert
        assertEquals(
            expected = ValidationResult.Valid,
            actual = actual,
            message = "InboundValidator should accept ROTATION_ANNOUNCEMENT frames with the expected fixed payload size",
        )
    }

    @Test
    public fun validate_rejectsRotationAnnouncementFramesWithUnexpectedPayloadLength(): Unit {
        // Arrange
        val validator = InboundValidator()
        val encoded: ByteArray = byteArrayOf(
            MessageType.ROTATION_ANNOUNCEMENT.code.toByte(),
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
            expected = ValidationFailureCode.ROTATION_ANNOUNCEMENT_PAYLOAD_SIZE_INVALID,
            actual = actual.code,
            message = "InboundValidator should reject ROTATION_ANNOUNCEMENT frames with malformed fixed payload sizes",
        )
        assertEquals(
            expected = "ROTATION_ANNOUNCEMENT payload must be exactly 128 bytes.",
            actual = actual.reason,
            message = "InboundValidator should explain ROTATION_ANNOUNCEMENT payload size violations clearly",
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
