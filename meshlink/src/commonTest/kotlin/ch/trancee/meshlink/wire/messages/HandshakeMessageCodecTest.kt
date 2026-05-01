package ch.trancee.meshlink.wire.messages

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class HandshakeMessageCodecTest {
  @Test
  public fun encodeAndDecode_roundTripHandshakeMessage(): Unit {
    // Arrange
    val expectedPayload: ByteArray = byteArrayOf(0x11, 0x22, 0x33, 0x44)
    val message = HandshakeMessage(round = HandshakeRound.TWO, payload = expectedPayload)

    // Act
    val encoded: ByteArray = HandshakeMessageCodec.encode(message = message)
    val decoded: HandshakeMessage = HandshakeMessageCodec.decode(payload = encoded)

    // Assert
    assertEquals(
      expected = HandshakeRound.TWO,
      actual = decoded.round,
      message = "HandshakeMessageCodec should preserve the handshake round",
    )
    assertContentEquals(
      expected = expectedPayload,
      actual = decoded.payload,
      message = "HandshakeMessageCodec should preserve the handshake payload bytes",
    )
  }

  @Test
  public fun encode_writesRoundCodeBeforePayloadBytes(): Unit {
    // Arrange
    val message = HandshakeMessage(round = HandshakeRound.THREE, payload = byteArrayOf(0x55, 0x66))

    // Act
    val encoded: ByteArray = HandshakeMessageCodec.encode(message = message)

    // Assert
    assertContentEquals(
      expected = byteArrayOf(HandshakeRound.THREE.code.toByte(), 0x55, 0x66),
      actual = encoded,
      message = "HandshakeMessageCodec should write the round byte before the handshake payload",
    )
  }

  @Test
  public fun decode_throwsWhenPayloadIsMissingRoundByte(): Unit {
    // Arrange
    val payload = ByteArray(size = 0)

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> { HandshakeMessageCodec.decode(payload = payload) }

    // Assert
    assertEquals(
      expected = "HANDSHAKE payload must contain at least the round byte.",
      actual = error.message,
      message =
        "HandshakeMessageCodec should reject payloads that do not contain the handshake round",
    )
  }

  @Test
  public fun decode_throwsWhenRoundCodeIsUnknown(): Unit {
    // Arrange
    val payload: ByteArray = byteArrayOf(0x7F, 0x01)

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> { HandshakeMessageCodec.decode(payload = payload) }

    // Assert
    assertEquals(
      expected = "Unknown handshake round code: 0x7f.",
      actual = error.message,
      message = "HandshakeMessageCodec should reject unknown handshake round values",
    )
  }
}
