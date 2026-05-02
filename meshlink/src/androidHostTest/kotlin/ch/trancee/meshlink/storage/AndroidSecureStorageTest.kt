package ch.trancee.meshlink.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class AndroidSecureStorageTest {
  @Test
  public fun putGetAndRemove_roundTripValuesByKey(): Unit {
    // Arrange
    val storage = AndroidSecureStorage()

    // Act
    storage.putString(key = "token", value = "secret")
    val stored = storage.getString(key = "token")
    storage.remove(key = "token")
    val removed = storage.getString(key = "token")

    // Assert
    assertEquals(expected = "secret", actual = stored)
    assertEquals(expected = null, actual = removed)
  }

  @Test
  public fun clear_erasesAllStoredValues(): Unit {
    // Arrange
    val storage = AndroidSecureStorage()
    storage.putString(key = "token", value = "secret")
    storage.putString(key = "device", value = "peer")

    // Act
    storage.clear()
    val token = storage.getString(key = "token")
    val device = storage.getString(key = "device")

    // Assert
    assertEquals(expected = null, actual = token)
    assertEquals(expected = null, actual = device)
  }

  @Test
  public fun blankKeys_areRejectedAcrossAllOperations(): Unit {
    // Arrange
    val storage = AndroidSecureStorage()

    // Act
    val putError =
      assertFailsWith<IllegalArgumentException> { storage.putString(key = "   ", value = "value") }
    val getError = assertFailsWith<IllegalArgumentException> { storage.getString(key = "") }
    val removeError = assertFailsWith<IllegalArgumentException> { storage.remove(key = "   ") }

    // Assert
    assertEquals(
      expected = "AndroidSecureStorage key must not be blank.",
      actual = putError.message,
    )
    assertEquals(
      expected = "AndroidSecureStorage key must not be blank.",
      actual = getError.message,
    )
    assertEquals(
      expected = "AndroidSecureStorage key must not be blank.",
      actual = removeError.message,
    )
  }
}
