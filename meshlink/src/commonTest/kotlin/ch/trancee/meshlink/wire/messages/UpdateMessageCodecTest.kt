package ch.trancee.meshlink.wire.messages

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class UpdateMessageCodecTest {
  @Test
  public fun encodeAndDecode_roundTripUpdateMessage(): Unit {
    // Arrange
    val expectedDestinationPeerId: ByteArray =
      byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C)
    val expectedMetric: Int = 42
    val expectedSeqno: Int = 7
    val message =
      UpdateMessage(
        destinationPeerId = expectedDestinationPeerId,
        metric = expectedMetric,
        seqno = expectedSeqno,
      )

    // Act
    val encoded: ByteArray = UpdateMessageCodec.encode(message = message)
    val decoded: UpdateMessage = UpdateMessageCodec.decode(payload = encoded)

    // Assert
    assertContentEquals(
      expected = expectedDestinationPeerId,
      actual = decoded.destinationPeerId,
      message = "UpdateMessageCodec should preserve the destination peer identifier bytes",
    )
    assertEquals(
      expected = expectedMetric,
      actual = decoded.metric,
      message = "UpdateMessageCodec should preserve the advertised metric",
    )
    assertEquals(
      expected = expectedSeqno,
      actual = decoded.seqno,
      message = "UpdateMessageCodec should preserve the advertised sequence number",
    )
  }

  @Test
  public fun encode_throwsWhenDestinationPeerIdLengthIsInvalid(): Unit {
    // Arrange
    val message = UpdateMessage(destinationPeerId = byteArrayOf(0x01), metric = 1, seqno = 2)

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> { UpdateMessageCodec.encode(message = message) }

    // Assert
    assertEquals(
      expected = "Update destinationPeerId must be exactly 12 bytes.",
      actual = error.message,
      message =
        "UpdateMessageCodec should reject destination peer identifiers with the wrong length",
    )
  }

  @Test
  public fun decode_throwsWhenPayloadLengthIsInvalid(): Unit {
    // Arrange
    val payload: ByteArray = byteArrayOf(0x01, 0x02, 0x03)

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> { UpdateMessageCodec.decode(payload = payload) }

    // Assert
    assertEquals(
      expected = "UPDATE payload must be exactly 20 bytes.",
      actual = error.message,
      message = "UpdateMessageCodec should reject malformed UPDATE payload sizes",
    )
  }
}
