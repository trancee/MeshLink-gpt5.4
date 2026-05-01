package ch.trancee.meshlink.crypto.noise

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

public class DhCacheTest {
  @Test
  public fun constructor_usesDefaultCapacityWhenNotSpecified(): Unit {
    // Arrange
    val cache = DhCache()
    var computeCalls: Int = 0

    // Act
    cache.getOrCompute(privateKey = byteArrayOf(0x01), publicKey = byteArrayOf(0x02)) {
      computeCalls += 1
      byteArrayOf(0x11)
    }
    cache.getOrCompute(privateKey = byteArrayOf(0x01), publicKey = byteArrayOf(0x02)) {
      computeCalls += 1
      byteArrayOf(0x22)
    }

    // Assert
    assertEquals(
      expected = 1,
      actual = computeCalls,
      message =
        "DhCache should use its default capacity and cache repeated entries when no explicit size is provided",
    )
  }

  @Test
  public fun constructor_normalizesNonPositiveCapacityToOneEntry(): Unit {
    // Arrange
    val cache = DhCache(maxEntries = 0)
    var computeCalls: Int = 0

    // Act
    cache.getOrCompute(privateKey = byteArrayOf(0x01), publicKey = byteArrayOf(0x02)) {
      computeCalls += 1
      byteArrayOf(0x11)
    }
    cache.getOrCompute(privateKey = byteArrayOf(0x03), publicKey = byteArrayOf(0x04)) {
      computeCalls += 1
      byteArrayOf(0x22)
    }
    cache.getOrCompute(privateKey = byteArrayOf(0x01), publicKey = byteArrayOf(0x02)) {
      computeCalls += 1
      byteArrayOf(0x33)
    }

    // Assert
    assertEquals(
      expected = 3,
      actual = computeCalls,
      message = "DhCache should normalize non-positive capacities to a single-entry cache",
    )
  }

  @Test
  public fun getOrCompute_returnsCachedResultForRepeatedKeyPair(): Unit {
    // Arrange
    val cache = DhCache(maxEntries = 2)
    var computeCalls: Int = 0
    val expectedSharedSecret: ByteArray = byteArrayOf(0x11, 0x22, 0x33)

    // Act
    val first: ByteArray =
      cache.getOrCompute(privateKey = byteArrayOf(0x01), publicKey = byteArrayOf(0x02)) {
        computeCalls += 1
        expectedSharedSecret
      }
    val second: ByteArray =
      cache.getOrCompute(privateKey = byteArrayOf(0x01), publicKey = byteArrayOf(0x02)) {
        computeCalls += 1
        byteArrayOf(0x44)
      }

    // Assert
    assertContentEquals(
      expected = expectedSharedSecret,
      actual = first,
      message = "DhCache should return the computed shared secret on the first lookup",
    )
    assertContentEquals(
      expected = expectedSharedSecret,
      actual = second,
      message = "DhCache should return the cached shared secret on repeated lookups",
    )
    assertEquals(
      expected = 1,
      actual = computeCalls,
      message = "DhCache should invoke the compute callback only once per unique key pair",
    )
  }

  @Test
  public fun getOrCompute_evictsLeastRecentlyUsedEntryWhenCapacityIsExceeded(): Unit {
    // Arrange
    val cache = DhCache(maxEntries = 1)
    var computeCalls: Int = 0

    // Act
    cache.getOrCompute(privateKey = byteArrayOf(0x01), publicKey = byteArrayOf(0x02)) {
      computeCalls += 1
      byteArrayOf(0x11)
    }
    cache.getOrCompute(privateKey = byteArrayOf(0x03), publicKey = byteArrayOf(0x04)) {
      computeCalls += 1
      byteArrayOf(0x22)
    }
    cache.getOrCompute(privateKey = byteArrayOf(0x01), publicKey = byteArrayOf(0x02)) {
      computeCalls += 1
      byteArrayOf(0x33)
    }

    // Assert
    assertEquals(
      expected = 3,
      actual = computeCalls,
      message = "DhCache should evict the least recently used entry once capacity is exceeded",
    )
  }

  @Test
  public fun getOrCompute_returnsDefensiveCopiesOfCachedValues(): Unit {
    // Arrange
    val cache = DhCache(maxEntries = 1)
    val initialSecret: ByteArray =
      cache.getOrCompute(privateKey = byteArrayOf(0x05), publicKey = byteArrayOf(0x06)) {
        byteArrayOf(0x21, 0x22)
      }
    initialSecret[0] = 0x7F

    // Act
    val actual: ByteArray =
      cache.getOrCompute(privateKey = byteArrayOf(0x05), publicKey = byteArrayOf(0x06)) {
        byteArrayOf(0x33, 0x44)
      }

    // Assert
    assertContentEquals(
      expected = byteArrayOf(0x21, 0x22),
      actual = actual,
      message = "DhCache should protect cached shared secrets from caller mutation",
    )
  }
}
