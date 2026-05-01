package ch.trancee.meshlink.transport

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

public class MeshHashFilterTest {
    @Test
    public fun defaultMaxEntries_matchesTheFilterContract(): Unit {
        // Arrange
        val expected = 128

        // Act
        val actual = MeshHashFilter.DEFAULT_MAX_ENTRIES

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    public fun isDuplicate_returnsFalseTheFirstTimeAHashIsSeen(): Unit {
        // Arrange
        val filter = MeshHashFilter()
        val meshHash = byteArrayOf(0x01, 0x02, 0x03)

        // Act
        val actual = filter.isDuplicate(meshHash = meshHash)

        // Assert
        assertFalse(
            actual = actual,
            message = "MeshHashFilter should accept the first advertisement hash it sees",
        )
    }

    @Test
    public fun isDuplicate_returnsTrueWhenTheSameHashIsSeenAgain(): Unit {
        // Arrange
        val filter = MeshHashFilter()
        val meshHash = byteArrayOf(0x01, 0x02, 0x03)
        filter.isDuplicate(meshHash = meshHash)

        // Act
        val actual = filter.isDuplicate(meshHash = meshHash)

        // Assert
        assertTrue(
            actual = actual,
            message = "MeshHashFilter should reject duplicate advertisement hashes",
        )
    }

    @Test
    public fun isDuplicate_evictsTheOldestHashWhenCapacityIsReached(): Unit {
        // Arrange
        val filter = MeshHashFilter(maxEntries = 2)
        val firstHash = byteArrayOf(0x01)
        val secondHash = byteArrayOf(0x02)
        val thirdHash = byteArrayOf(0x03)
        filter.isDuplicate(meshHash = firstHash)
        filter.isDuplicate(meshHash = secondHash)
        filter.isDuplicate(meshHash = thirdHash)

        // Act
        val actual = filter.isDuplicate(meshHash = firstHash)

        // Assert
        assertFalse(
            actual = actual,
            message = "MeshHashFilter should evict the oldest hash once the dedup window is full",
        )
    }

    @Test
    public fun isDuplicate_refreshesExistingHashesSoRecentlySeenEntriesStayCached(): Unit {
        // Arrange
        val filter = MeshHashFilter(maxEntries = 2)
        val firstHash = byteArrayOf(0x01)
        val secondHash = byteArrayOf(0x02)
        val thirdHash = byteArrayOf(0x03)
        filter.isDuplicate(meshHash = firstHash)
        filter.isDuplicate(meshHash = secondHash)
        filter.isDuplicate(meshHash = firstHash)
        filter.isDuplicate(meshHash = thirdHash)

        // Act
        val firstActual = filter.isDuplicate(meshHash = firstHash)
        val secondActual = filter.isDuplicate(meshHash = secondHash)

        // Assert
        assertTrue(actual = firstActual)
        assertFalse(actual = secondActual)
    }

    @Test
    public fun clear_forgetsPreviouslySeenHashes(): Unit {
        // Arrange
        val filter = MeshHashFilter()
        val meshHash = byteArrayOf(0x0A)
        filter.isDuplicate(meshHash = meshHash)
        filter.clear()

        // Act
        val actual = filter.isDuplicate(meshHash = meshHash)

        // Assert
        assertFalse(
            actual = actual,
            message = "MeshHashFilter should forget hashes after clear is called",
        )
    }

    @Test
    public fun isDuplicate_rejectsEmptyHashes(): Unit {
        // Arrange
        val filter = MeshHashFilter()

        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            filter.isDuplicate(meshHash = byteArrayOf())
        }

        // Assert
        assertEquals(
            expected = "MeshHashFilter meshHash must not be empty.",
            actual = error.message,
        )
    }

    @Test
    public fun init_rejectsNonPositiveCapacity(): Unit {
        // Arrange
        val expectedMessage = "MeshHashFilter maxEntries must be greater than 0."

        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            MeshHashFilter(maxEntries = 0)
        }

        // Assert
        assertEquals(expected = expectedMessage, actual = error.message)
    }
}
