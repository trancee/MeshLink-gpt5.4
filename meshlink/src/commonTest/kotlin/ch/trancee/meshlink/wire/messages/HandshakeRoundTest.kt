package ch.trancee.meshlink.wire.messages

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class HandshakeRoundTest {
  @Test
  public fun fromCode_returnsMatchingHandshakeRound(): Unit {
    // Arrange
    val code: UByte = 0x02u

    // Act
    val actual: HandshakeRound = HandshakeRound.fromCode(code = code)

    // Assert
    assertEquals(
      expected = HandshakeRound.TWO,
      actual = actual,
      message = "HandshakeRound should decode known handshake round codes",
    )
  }

  @Test
  public fun fromCode_throwsForUnknownHandshakeRoundCode(): Unit {
    // Arrange
    val code: UByte = 0x7Fu

    // Act
    val error = assertFailsWith<IllegalArgumentException> { HandshakeRound.fromCode(code = code) }

    // Assert
    assertEquals(
      expected = "Unknown handshake round code: 0x7f.",
      actual = error.message,
      message = "HandshakeRound should reject unknown handshake round codes",
    )
  }
}
