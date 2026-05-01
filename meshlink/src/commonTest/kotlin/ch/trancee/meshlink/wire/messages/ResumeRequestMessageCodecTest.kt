package ch.trancee.meshlink.wire.messages

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class ResumeRequestMessageCodecTest {
  @Test
  public fun encodeAndDecode_roundTripResumeRequestMessage(): Unit {
    // Arrange
    val expectedTransferId: Long = 0x0102030405060708
    val expectedResumeOffset: Long = 0x1122334455667788
    val message =
      ResumeRequestMessage(transferId = expectedTransferId, resumeOffset = expectedResumeOffset)

    // Act
    val encoded: ByteArray = ResumeRequestMessageCodec.encode(message = message)
    val decoded: ResumeRequestMessage = ResumeRequestMessageCodec.decode(payload = encoded)

    // Assert
    assertEquals(
      expected = expectedTransferId,
      actual = decoded.transferId,
      message = "ResumeRequestMessageCodec should preserve the transfer identifier",
    )
    assertEquals(
      expected = expectedResumeOffset,
      actual = decoded.resumeOffset,
      message = "ResumeRequestMessageCodec should preserve the requested resume offset",
    )
  }

  @Test
  public fun decode_throwsWhenPayloadLengthIsInvalid(): Unit {
    // Arrange
    val payload: ByteArray = byteArrayOf(0x01)

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> {
        ResumeRequestMessageCodec.decode(payload = payload)
      }

    // Assert
    assertEquals(
      expected = "RESUME_REQUEST payload must be exactly 16 bytes.",
      actual = error.message,
      message = "ResumeRequestMessageCodec should reject malformed RESUME_REQUEST payload sizes",
    )
  }
}
