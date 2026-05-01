package ch.trancee.meshlink.wire

import ch.trancee.meshlink.wire.messages.BroadcastMessageCodec
import ch.trancee.meshlink.wire.messages.HelloMessageCodec
import ch.trancee.meshlink.wire.messages.RotationAnnouncementMessageCodec
import ch.trancee.meshlink.wire.messages.UpdateMessageCodec

/**
 * Performs cheap structural validation on encoded frames before full decoding.
 *
 * This lets callers reject malformed or obviously dangerous input without allocating message
 * objects for every invalid packet.
 */
public class InboundValidator(private val maxPayloadSize: Int = DEFAULT_MAX_PAYLOAD_SIZE) {
  /** Validates the frame header first, then applies message-type-specific size checks. */
  public fun validate(encoded: ByteArray): ValidationResult {
    if (encoded.size < HEADER_SIZE) {
      return ValidationResult.Invalid(
        code = ValidationFailureCode.TRUNCATED_HEADER,
        reason = "Encoded frame is shorter than the 5-byte header.",
      )
    }

    val readBuffer = ReadBuffer(source = encoded)
    val typeCode: UByte = readBuffer.readByte().toUByte()
    val type: MessageType =
      try {
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
        reason =
          "Encoded frame payload exceeds the configured maximum size of $maxPayloadSize bytes.",
      )
    }

    val expectedSize: Int = HEADER_SIZE + payloadSize
    if (encoded.size != expectedSize) {
      return ValidationResult.Invalid(
        code = ValidationFailureCode.PAYLOAD_LENGTH_MISMATCH,
        reason =
          "Encoded frame length mismatch: expected $expectedSize bytes but received ${encoded.size}.",
      )
    }

    return when (type) {
      MessageType.HELLO -> validateHello(payloadSize = payloadSize)
      MessageType.HANDSHAKE -> validateHandshake(payloadSize = payloadSize)
      MessageType.UPDATE ->
        validateFixedPayload(
          payloadSize = payloadSize,
          expectedPayloadSize =
            UpdateMessageCodec.DESTINATION_PEER_ID_SIZE + Int.SIZE_BYTES + Int.SIZE_BYTES,
          failureCode = ValidationFailureCode.UPDATE_PAYLOAD_SIZE_INVALID,
          failureReason =
            "UPDATE payload must be exactly ${UpdateMessageCodec.DESTINATION_PEER_ID_SIZE + Int.SIZE_BYTES + Int.SIZE_BYTES} bytes.",
        )
      MessageType.CHUNK -> validateChunk(payloadSize = payloadSize)
      MessageType.CHUNK_ACK -> validateChunkAck(payloadSize = payloadSize)
      MessageType.DELIVERY_ACK ->
        validateFixedPayload(
          payloadSize = payloadSize,
          expectedPayloadSize = Long.SIZE_BYTES,
          failureCode = ValidationFailureCode.DELIVERY_ACK_PAYLOAD_SIZE_INVALID,
          failureReason = "DELIVERY_ACK payload must be exactly ${Long.SIZE_BYTES} bytes.",
        )
      MessageType.NACK ->
        validateFixedPayload(
          payloadSize = payloadSize,
          expectedPayloadSize = Long.SIZE_BYTES + Int.SIZE_BYTES,
          failureCode = ValidationFailureCode.NACK_PAYLOAD_SIZE_INVALID,
          failureReason = "NACK payload must be exactly ${Long.SIZE_BYTES + Int.SIZE_BYTES} bytes.",
        )
      MessageType.KEEPALIVE ->
        validateFixedPayload(
          payloadSize = payloadSize,
          expectedPayloadSize = 0,
          failureCode = ValidationFailureCode.KEEPALIVE_PAYLOAD_NOT_EMPTY,
          failureReason = "KEEPALIVE payload must be empty.",
        )
      MessageType.BROADCAST -> validateBroadcast(encoded = encoded, payloadSize = payloadSize)
      MessageType.ROUTED_MESSAGE ->
        validateRoutedMessage(encoded = encoded, payloadSize = payloadSize)
      MessageType.RESUME_REQUEST ->
        validateFixedPayload(
          payloadSize = payloadSize,
          expectedPayloadSize = Long.SIZE_BYTES + Long.SIZE_BYTES,
          failureCode = ValidationFailureCode.RESUME_REQUEST_PAYLOAD_SIZE_INVALID,
          failureReason =
            "RESUME_REQUEST payload must be exactly ${Long.SIZE_BYTES + Long.SIZE_BYTES} bytes.",
        )
      MessageType.ROTATION_ANNOUNCEMENT ->
        validateFixedPayload(
          payloadSize = payloadSize,
          expectedPayloadSize =
            RotationAnnouncementMessageCodec.PUBLIC_KEY_SIZE +
              RotationAnnouncementMessageCodec.PUBLIC_KEY_SIZE +
              RotationAnnouncementMessageCodec.SIGNATURE_SIZE,
          failureCode = ValidationFailureCode.ROTATION_ANNOUNCEMENT_PAYLOAD_SIZE_INVALID,
          failureReason =
            "ROTATION_ANNOUNCEMENT payload must be exactly ${RotationAnnouncementMessageCodec.PUBLIC_KEY_SIZE + RotationAnnouncementMessageCodec.PUBLIC_KEY_SIZE + RotationAnnouncementMessageCodec.SIGNATURE_SIZE} bytes.",
        )
    }
  }

  private fun validateHello(payloadSize: Int): ValidationResult {
    return if (payloadSize == HelloMessageCodec.PEER_ID_SIZE + Int.SIZE_BYTES) {
      ValidationResult.Valid
    } else {
      ValidationResult.Invalid(
        code = ValidationFailureCode.HELLO_PAYLOAD_SIZE_INVALID,
        reason =
          "HELLO payload must be exactly ${HelloMessageCodec.PEER_ID_SIZE + Int.SIZE_BYTES} bytes.",
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

  private fun validateFixedPayload(
    payloadSize: Int,
    expectedPayloadSize: Int,
    failureCode: ValidationFailureCode,
    failureReason: String,
  ): ValidationResult {
    return if (payloadSize == expectedPayloadSize) {
      ValidationResult.Valid
    } else {
      ValidationResult.Invalid(code = failureCode, reason = failureReason)
    }
  }

  private fun validateChunk(payloadSize: Int): ValidationResult {
    return if (payloadSize >= CHUNK_HEADER_SIZE) {
      ValidationResult.Valid
    } else {
      ValidationResult.Invalid(
        code = ValidationFailureCode.CHUNK_PAYLOAD_TOO_SHORT,
        reason = "CHUNK payload must contain transferId and chunkIndex.",
      )
    }
  }

  private fun validateChunkAck(payloadSize: Int): ValidationResult {
    return if (payloadSize >= CHUNK_ACK_HEADER_SIZE) {
      ValidationResult.Valid
    } else {
      ValidationResult.Invalid(
        code = ValidationFailureCode.CHUNK_ACK_PAYLOAD_TOO_SHORT,
        reason = "CHUNK_ACK payload must contain transferId and highestContiguousChunkIndex.",
      )
    }
  }

  private fun validateBroadcast(encoded: ByteArray, payloadSize: Int): ValidationResult {
    if (payloadSize < BROADCAST_HEADER_SIZE) {
      return ValidationResult.Invalid(
        code = ValidationFailureCode.BROADCAST_PAYLOAD_TOO_SHORT,
        reason = "BROADCAST payload must contain originPeerId, sequenceNumber, and maxHops.",
      )
    }

    val payloadOffset: Int = HEADER_SIZE
    val maxHops: UByte =
      encoded[payloadOffset + HelloMessageCodec.PEER_ID_SIZE + Int.SIZE_BYTES].toUByte()
    return if (maxHops > 0u) {
      ValidationResult.Valid
    } else {
      ValidationResult.Invalid(
        code = ValidationFailureCode.BROADCAST_MAX_HOPS_INVALID,
        reason = "BROADCAST maxHops must be greater than zero.",
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
    private const val CHUNK_HEADER_SIZE: Int = Long.SIZE_BYTES + Int.SIZE_BYTES
    private const val CHUNK_ACK_HEADER_SIZE: Int = Long.SIZE_BYTES + Int.SIZE_BYTES
    private const val BROADCAST_HEADER_SIZE: Int =
      BroadcastMessageCodec.ORIGIN_PEER_ID_SIZE + Int.SIZE_BYTES + 1
    private const val ROUTED_HEADER_SIZE: Int = 2
  }
}
