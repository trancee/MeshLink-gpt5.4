package ch.trancee.meshlink.api

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

public class PeerIdHexTest {
  @Test
  public fun fromBytes_roundTripsThroughToByteArray(): Unit {
    // Arrange
    val expectedBytes: ByteArray = byteArrayOf(0x01, 0x2A, 0x7F.toByte())

    // Act
    val actual: ByteArray = PeerIdHex.fromBytes(bytes = expectedBytes).toByteArray()

    // Assert
    assertContentEquals(
      expected = expectedBytes,
      actual = actual,
      message = "PeerIdHex should round-trip between bytes and hexadecimal form",
    )
  }

  @Test
  public fun value_retainsOriginalHexString(): Unit {
    // Arrange
    val peerId = PeerIdHex(value = "0a0b")

    // Act
    val actual: String = peerId.value

    // Assert
    assertEquals(
      expected = "0a0b",
      actual = actual,
      message = "PeerIdHex should retain the original hexadecimal string",
    )
  }

  @Test
  public fun constructor_acceptsDigitOnlyHexCharacters(): Unit {
    // Arrange
    val expectedBytes: ByteArray = byteArrayOf(0x12, 0x34)

    // Act
    val actual: ByteArray = PeerIdHex(value = "1234").toByteArray()

    // Assert
    assertContentEquals(
      expected = expectedBytes,
      actual = actual,
      message = "PeerIdHex should accept digit-only hexadecimal identifiers",
    )
  }

  @Test
  public fun constructor_acceptsUppercaseHexCharacters(): Unit {
    // Arrange
    val expectedBytes: ByteArray = byteArrayOf(0xAB.toByte(), 0xCD.toByte())

    // Act
    val actual: ByteArray = PeerIdHex(value = "ABCD").toByteArray()

    // Assert
    assertContentEquals(
      expected = expectedBytes,
      actual = actual,
      message = "PeerIdHex should accept uppercase hexadecimal characters",
    )
  }

  @Test
  public fun constructor_throwsWhenValueIsEmpty(): Unit {
    // Arrange

    // Act
    val error = assertFailsWith<IllegalArgumentException> { PeerIdHex(value = "") }

    // Assert
    assertEquals(
      expected = "PeerIdHex must not be empty.",
      actual = error.message,
      message = "PeerIdHex should reject empty identifiers",
    )
  }

  @Test
  public fun constructor_throwsWhenLengthIsOdd(): Unit {
    // Arrange

    // Act
    val error = assertFailsWith<IllegalArgumentException> { PeerIdHex(value = "abc") }

    // Assert
    assertEquals(
      expected = "PeerIdHex must contain an even number of hex characters.",
      actual = error.message,
      message = "PeerIdHex should reject identifiers with odd-length hex strings",
    )
  }

  @Test
  public fun constructor_throwsWhenCharactersAreNotHexadecimal(): Unit {
    // Arrange

    // Act
    val error = assertFailsWith<IllegalArgumentException> { PeerIdHex(value = "zz") }

    // Assert
    assertEquals(
      expected = "PeerIdHex must contain only hexadecimal characters.",
      actual = error.message,
      message = "PeerIdHex should reject identifiers with non-hexadecimal characters",
    )
  }

  @Test
  public fun constructor_throwsWhenValueIsNullAtRuntime(): Unit {
    // Arrange
    val constructor = PeerIdHex::class.java.getConstructor(String::class.java)

    // Act
    val error =
      assertFailsWith<java.lang.reflect.InvocationTargetException> { constructor.newInstance(null) }

    // Assert
    assertTrue(
      actual = error.cause is NullPointerException,
      message = "PeerIdHex should reject null constructor values at runtime",
    )
  }

  @Test
  public fun constructor_throwsWhenLaterCharactersAreNotHexadecimal(): Unit {
    // Arrange

    // Act
    val error = assertFailsWith<IllegalArgumentException> { PeerIdHex(value = "0g") }

    // Assert
    assertEquals(
      expected = "PeerIdHex must contain only hexadecimal characters.",
      actual = error.message,
      message = "PeerIdHex should reject identifiers when a later character is not hexadecimal",
    )
  }
}
