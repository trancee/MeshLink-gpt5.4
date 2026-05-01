package ch.trancee.meshlink.wire.messages

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class KeepaliveMessageCodecTest {
  @Test
  public fun encode_returnsEmptyPayload(): Unit {
    // Arrange
    val expectedPayload = ByteArray(size = 0)

    // Act
    val actualPayload: ByteArray = KeepaliveMessageCodec.encode()

    // Assert
    assertContentEquals(
      expected = expectedPayload,
      actual = actualPayload,
      message = "KeepaliveMessageCodec should encode KEEPALIVE as an empty payload",
    )
  }

  @Test
  public fun decode_returnsKeepaliveMessageForEmptyPayload(): Unit {
    // Arrange
    val payload = ByteArray(size = 0)

    // Act
    val actual: KeepaliveMessage = KeepaliveMessageCodec.decode(payload = payload)

    // Assert
    assertEquals(
      expected = KeepaliveMessage,
      actual = actual,
      message = "KeepaliveMessageCodec should decode an empty payload as KEEPALIVE",
    )
  }

  @Test
  public fun decode_throwsWhenPayloadIsNotEmpty(): Unit {
    // Arrange
    val payload: ByteArray = byteArrayOf(0x01)

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> { KeepaliveMessageCodec.decode(payload = payload) }

    // Assert
    assertEquals(
      expected = "KEEPALIVE payload must be empty.",
      actual = error.message,
      message = "KeepaliveMessageCodec should reject KEEPALIVE payload bytes",
    )
  }
}
