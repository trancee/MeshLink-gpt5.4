package ch.trancee.meshlink.crypto

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

public class ConstantTimeEqualsTest {
  @Test
  public fun bytes_returnsTrueForEqualArrays(): Unit {
    // Arrange
    val left: ByteArray = byteArrayOf(0x01, 0x02, 0x03)
    val right: ByteArray = byteArrayOf(0x01, 0x02, 0x03)

    // Act
    val actual: Boolean = ConstantTimeEquals.bytes(left = left, right = right)

    // Assert
    assertTrue(actual = actual, message = "ConstantTimeEquals should report equal arrays as equal")
  }

  @Test
  public fun bytes_returnsFalseForArraysWithDifferentContent(): Unit {
    // Arrange
    val left: ByteArray = byteArrayOf(0x01, 0x02, 0x03)
    val right: ByteArray = byteArrayOf(0x01, 0x02, 0x04)

    // Act
    val actual: Boolean = ConstantTimeEquals.bytes(left = left, right = right)

    // Assert
    assertFalse(
      actual = actual,
      message = "ConstantTimeEquals should reject arrays with different byte values",
    )
  }

  @Test
  public fun bytes_returnsFalseForArraysWithDifferentLengths(): Unit {
    // Arrange
    val left: ByteArray = byteArrayOf(0x01, 0x02)
    val right: ByteArray = byteArrayOf(0x01, 0x02, 0x03)

    // Act
    val actual: Boolean = ConstantTimeEquals.bytes(left = left, right = right)

    // Assert
    assertFalse(
      actual = actual,
      message = "ConstantTimeEquals should reject arrays with different lengths",
    )
  }

  @Test
  public fun bytes_returnsFalseWhenLeftArrayIsLongerThanRightArray(): Unit {
    // Arrange
    val left: ByteArray = byteArrayOf(0x01, 0x02, 0x03)
    val right: ByteArray = byteArrayOf(0x01, 0x02)

    // Act
    val actual: Boolean = ConstantTimeEquals.bytes(left = left, right = right)

    // Assert
    assertFalse(
      actual = actual,
      message =
        "ConstantTimeEquals should reject arrays when the left side is longer than the right side",
    )
  }
}
