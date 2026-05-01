package ch.trancee.meshlink.routing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

public class DedupSetTest {
  @Test
  public fun defaults_matchTheDedupContract(): Unit {
    // Arrange
    // Act
    val defaultMaxEntries = DedupSet.DEFAULT_MAX_ENTRIES
    val defaultExpiryMillis = DedupSet.DEFAULT_EXPIRY_MILLIS

    // Assert
    assertEquals(expected = 256, actual = defaultMaxEntries)
    assertEquals(expected = 30_000L, actual = defaultExpiryMillis)
  }

  @Test
  public fun isDuplicate_returnsFalseWhenAKeyIsSeenForTheFirstTime(): Unit {
    // Arrange
    val dedupSet = DedupSet()

    // Act
    val actual = dedupSet.isDuplicate(key = byteArrayOf(0x01), nowEpochMillis = 1L)

    // Assert
    assertFalse(actual = actual)
    assertEquals(expected = 1, actual = dedupSet.size())
  }

  @Test
  public fun isDuplicate_returnsTrueWhenAKeyIsSeenAgainBeforeExpiry(): Unit {
    // Arrange
    val dedupSet = DedupSet(expiryMillis = 100L)
    dedupSet.isDuplicate(key = byteArrayOf(0x01), nowEpochMillis = 1L)

    // Act
    val actual = dedupSet.isDuplicate(key = byteArrayOf(0x01), nowEpochMillis = 50L)

    // Assert
    assertTrue(actual = actual)
    assertEquals(expected = 1, actual = dedupSet.size())
  }

  @Test
  public fun isDuplicate_expiresKeysAfterTheConfiguredWindow(): Unit {
    // Arrange
    val dedupSet = DedupSet(expiryMillis = 100L)
    dedupSet.isDuplicate(key = byteArrayOf(0x01), nowEpochMillis = 0L)

    // Act
    val actual = dedupSet.isDuplicate(key = byteArrayOf(0x01), nowEpochMillis = 100L)

    // Assert
    assertFalse(actual = actual)
    assertEquals(expected = 1, actual = dedupSet.size())
  }

  @Test
  public fun isDuplicate_evictsTheLeastRecentlyUsedEntryWhenCapacityIsReached(): Unit {
    // Arrange
    val dedupSet = DedupSet(maxEntries = 2, expiryMillis = 1_000L)
    dedupSet.isDuplicate(key = byteArrayOf(0x01), nowEpochMillis = 0L)
    dedupSet.isDuplicate(key = byteArrayOf(0x02), nowEpochMillis = 1L)
    dedupSet.isDuplicate(key = byteArrayOf(0x01), nowEpochMillis = 2L)

    // Act
    dedupSet.isDuplicate(key = byteArrayOf(0x03), nowEpochMillis = 3L)
    val refreshedActual = dedupSet.isDuplicate(key = byteArrayOf(0x01), nowEpochMillis = 4L)
    val evictedActual = dedupSet.isDuplicate(key = byteArrayOf(0x02), nowEpochMillis = 5L)

    // Assert
    assertTrue(actual = refreshedActual)
    assertFalse(actual = evictedActual)
    assertEquals(expected = 2, actual = dedupSet.size())
  }

  @Test
  public fun isDuplicate_rejectsInvalidInputs(): Unit {
    // Arrange
    val dedupSet = DedupSet()

    // Act
    val emptyKeyError =
      assertFailsWith<IllegalArgumentException> {
        dedupSet.isDuplicate(key = byteArrayOf(), nowEpochMillis = 0L)
      }
    val negativeTimestampError =
      assertFailsWith<IllegalArgumentException> {
        dedupSet.isDuplicate(key = byteArrayOf(0x01), nowEpochMillis = -1L)
      }

    // Assert
    assertEquals(expected = "DedupSet key must not be empty.", actual = emptyKeyError.message)
    assertEquals(
      expected = "DedupSet nowEpochMillis must be greater than or equal to 0.",
      actual = negativeTimestampError.message,
    )
  }

  @Test
  public fun init_rejectsNonPositiveCapacityAndExpiryValues(): Unit {
    // Arrange
    // Act
    val maxEntriesError = assertFailsWith<IllegalArgumentException> { DedupSet(maxEntries = 0) }
    val expiryError = assertFailsWith<IllegalArgumentException> { DedupSet(expiryMillis = 0L) }

    // Assert
    assertEquals(
      expected = "DedupSet maxEntries must be greater than 0.",
      actual = maxEntriesError.message,
    )
    assertEquals(
      expected = "DedupSet expiryMillis must be greater than 0.",
      actual = expiryError.message,
    )
  }
}
