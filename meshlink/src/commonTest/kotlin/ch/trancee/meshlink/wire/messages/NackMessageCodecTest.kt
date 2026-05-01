package ch.trancee.meshlink.wire.messages

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class NackMessageCodecTest {
  @Test
  public fun encodeAndDecode_roundTripNackMessage(): Unit {
    // Arrange
    val expectedMessageId: Long = 0x8877665544332211uL.toLong()
    val expectedReasonCode: Int = 409
    val message = NackMessage(messageId = expectedMessageId, reasonCode = expectedReasonCode)

    // Act
    val encoded: ByteArray = NackMessageCodec.encode(message = message)
    val decoded: NackMessage = NackMessageCodec.decode(payload = encoded)

    // Assert
    assertEquals(
      expected = expectedMessageId,
      actual = decoded.messageId,
      message = "NackMessageCodec should preserve the rejected message identifier",
    )
    assertEquals(
      expected = expectedReasonCode,
      actual = decoded.reasonCode,
      message = "NackMessageCodec should preserve the reason code",
    )
  }

  @Test
  public fun decode_throwsWhenPayloadLengthIsInvalid(): Unit {
    // Arrange
    val payload: ByteArray = byteArrayOf(0x01)

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> { NackMessageCodec.decode(payload = payload) }

    // Assert
    assertEquals(
      expected = "NACK payload must be exactly 12 bytes.",
      actual = error.message,
      message = "NackMessageCodec should reject malformed NACK payload sizes",
    )
  }
}
