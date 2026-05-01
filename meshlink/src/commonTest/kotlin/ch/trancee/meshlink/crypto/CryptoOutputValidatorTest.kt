package ch.trancee.meshlink.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

public class CryptoOutputValidatorTest {
  @Test
  public fun requireExactSize_returnsACopyWhenTheSizeMatches(): Unit {
    // Arrange
    val original = byteArrayOf(0x01, 0x02, 0x03)

    // Act
    val actual: ByteArray =
      CryptoOutputValidator.requireExactSize(
        bytes = original,
        expectedSize = 3,
        label = "test bytes",
      )
    actual[0] = 0x7F

    // Assert
    assertContentEquals(expected = byteArrayOf(0x7F, 0x02, 0x03), actual = actual)
    assertContentEquals(expected = byteArrayOf(0x01, 0x02, 0x03), actual = original)
    assertFalse(actual === original, message = "Validator should return a defensive copy")
  }

  @Test
  public fun requireExactSize_throwsWhenTheSizeDoesNotMatch(): Unit {
    // Arrange
    val original = byteArrayOf(0x01, 0x02)

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> {
        CryptoOutputValidator.requireExactSize(
          bytes = original,
          expectedSize = 3,
          label = "HKDF-SHA256 output",
        )
      }

    // Assert
    assertEquals(expected = "HKDF-SHA256 output must be exactly 3 bytes.", actual = error.message)
  }
}
