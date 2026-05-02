package ch.trancee.meshlink.transport

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class OemL2capProbeCacheTest {
  @Test
  public fun defaultMaxEntries_matchesTheCacheContract(): Unit {
    // Arrange
    val expected = 64

    // Act
    val actual = OemL2capProbeCache.DEFAULT_MAX_ENTRIES

    // Assert
    assertEquals(expected = expected, actual = actual)
  }

  @Test
  public fun get_returnsNullWhenTheDeviceModelHasNotBeenProbedYet(): Unit {
    // Arrange
    val cache = OemL2capProbeCache()

    // Act
    val actual = cache.get(deviceModel = "Pixel 9")

    // Assert
    assertEquals(expected = null, actual = actual)
  }

  @Test
  public fun record_andGet_roundTripTheCachedCapability(): Unit {
    // Arrange
    val cache = OemL2capProbeCache()
    cache.record(deviceModel = "Pixel 9", supportsL2cap = true)

    // Act
    val actual = cache.get(deviceModel = "Pixel 9")

    // Assert
    assertEquals(expected = true, actual = actual)
  }

  @Test
  public fun record_normalizesDeviceModelsByWhitespaceAndCase(): Unit {
    // Arrange
    val cache = OemL2capProbeCache()
    cache.record(deviceModel = "  Pixel 9 Pro  ", supportsL2cap = false)

    // Act
    val actual = cache.get(deviceModel = "pixel 9 pro")

    // Assert
    assertEquals(expected = false, actual = actual)
  }

  @Test
  public fun record_updatesExistingEntriesInPlace(): Unit {
    // Arrange
    val cache = OemL2capProbeCache()
    cache.record(deviceModel = "Pixel 9", supportsL2cap = false)
    cache.record(deviceModel = "Pixel 9", supportsL2cap = true)

    // Act
    val actual = cache.get(deviceModel = "Pixel 9")

    // Assert
    assertEquals(expected = true, actual = actual)
  }

  @Test
  public fun record_evictsTheOldestEntryWhenCapacityIsReached(): Unit {
    // Arrange
    val cache = OemL2capProbeCache(maxEntries = 2)
    cache.record(deviceModel = "Pixel 9", supportsL2cap = true)
    cache.record(deviceModel = "iPhone 16", supportsL2cap = false)
    cache.record(deviceModel = "Galaxy S25", supportsL2cap = true)

    // Act
    val evicted = cache.get(deviceModel = "Pixel 9")
    val retained = cache.get(deviceModel = "Galaxy S25")

    // Assert
    assertEquals(expected = null, actual = evicted)
    assertEquals(expected = true, actual = retained)
  }

  @Test
  public fun get_refreshesEntriesSoRecentlyReadModelsStayCached(): Unit {
    // Arrange
    val cache = OemL2capProbeCache(maxEntries = 2)
    cache.record(deviceModel = "Pixel 9", supportsL2cap = true)
    cache.record(deviceModel = "iPhone 16", supportsL2cap = false)
    cache.get(deviceModel = "Pixel 9")
    cache.record(deviceModel = "Galaxy S25", supportsL2cap = true)

    // Act
    val refreshed = cache.get(deviceModel = "Pixel 9")
    val evicted = cache.get(deviceModel = "iPhone 16")

    // Assert
    assertEquals(expected = true, actual = refreshed)
    assertEquals(expected = null, actual = evicted)
  }

  @Test
  public fun clear_forgetsAllProbeResults(): Unit {
    // Arrange
    val cache = OemL2capProbeCache()
    cache.record(deviceModel = "Pixel 9", supportsL2cap = true)
    cache.clear()

    // Act
    val actual = cache.get(deviceModel = "Pixel 9")

    // Assert
    assertEquals(expected = null, actual = actual)
  }

  @Test
  public fun get_rejectsBlankDeviceModels(): Unit {
    // Arrange
    val cache = OemL2capProbeCache()

    // Act
    val error = assertFailsWith<IllegalArgumentException> { cache.get(deviceModel = "   ") }

    // Assert
    assertEquals(
      expected = "OemL2capProbeCache deviceModel must not be blank.",
      actual = error.message,
    )
  }

  @Test
  public fun init_rejectsNonPositiveCapacity(): Unit {
    // Arrange
    val expectedMessage = "OemL2capProbeCache maxEntries must be greater than 0."

    // Act
    val error = assertFailsWith<IllegalArgumentException> { OemL2capProbeCache(maxEntries = 0) }

    // Assert
    assertEquals(expected = expectedMessage, actual = error.message)
  }

  @Test
  public fun probe_marksEntriesAsStaleWhenTheirObservationWindowExpires(): Unit {
    // Arrange
    val cache = OemL2capProbeCache()
    cache.recordProbe(deviceModel = "Pixel 9", supportsL2cap = false, observedAtEpochMillis = 10L)

    // Act
    val actual = cache.probe(deviceModel = "Pixel 9", nowEpochMillis = 100L, maxAgeMillis = 25L)

    // Assert
    assertEquals(expected = false, actual = actual?.supportsL2cap)
    assertEquals(
      expected = true,
      actual = actual?.isStale,
      message =
        "OemL2capProbeCache should mark entries stale once they exceed the freshness window.",
    )
  }

  @Test
  public fun probe_keepsEntriesFreshWhenTheirObservationWindowIsStillValid(): Unit {
    // Arrange
    val cache = OemL2capProbeCache()
    cache.recordProbe(deviceModel = "Pixel 9", supportsL2cap = true, observedAtEpochMillis = 10L)

    // Act
    val actual = cache.probe(deviceModel = "Pixel 9", nowEpochMillis = 20L, maxAgeMillis = 25L)

    // Assert
    assertEquals(expected = true, actual = actual?.supportsL2cap)
    assertEquals(
      expected = false,
      actual = actual?.isStale,
      message = "OemL2capProbeCache should keep recent observations fresh.",
    )
  }
}
