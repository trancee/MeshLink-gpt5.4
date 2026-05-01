package ch.trancee.meshlink.wire.messages

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class DeliveryAckMessageCodecTest {
  @Test
  public fun encodeAndDecode_roundTripDeliveryAckMessage(): Unit {
    // Arrange
    val expectedMessageId: Long = 0x8877665544332211uL.toLong()
    val message = DeliveryAckMessage(messageId = expectedMessageId)

    // Act
    val encoded = DeliveryAckMessageCodec.encode(message = message)
    val decoded = DeliveryAckMessageCodec.decode(payload = encoded)

    // Assert
    assertEquals(
      expected = expectedMessageId,
      actual = decoded.messageId,
      message = "DeliveryAckMessageCodec should preserve the delivered message identifier",
    )
  }

  @Test
  public fun decode_throwsWhenPayloadLengthIsInvalid(): Unit {
    // Arrange
    val payload: ByteArray = byteArrayOf(0x01)

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> {
        DeliveryAckMessageCodec.decode(payload = payload)
      }

    // Assert
    assertEquals(
      expected = "DELIVERY_ACK payload must be exactly 8 bytes.",
      actual = error.message,
      message = "DeliveryAckMessageCodec should reject malformed DELIVERY_ACK payload sizes",
    )
  }
}
