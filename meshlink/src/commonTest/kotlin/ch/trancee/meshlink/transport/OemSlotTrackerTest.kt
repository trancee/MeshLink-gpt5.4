package ch.trancee.meshlink.transport

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class OemSlotTrackerTest {
  @Test
  public fun defaultMaxSlots_matchesTheTrackerContract(): Unit {
    // Arrange
    val expected = 4

    // Act
    val actual = OemSlotTracker.DEFAULT_MAX_SLOTS

    // Assert
    assertEquals(expected = expected, actual = actual)
  }

  @Test
  public fun acquire_assignsTheLowestAvailableSlot(): Unit {
    // Arrange
    val tracker = OemSlotTracker(maxSlots = 3)

    // Act
    val first = tracker.acquire(ownerId = "pixel-9")
    val second = tracker.acquire(ownerId = "iphone-16")

    // Assert
    assertEquals(expected = 0, actual = first)
    assertEquals(expected = 1, actual = second)
    assertEquals(expected = 2, actual = tracker.occupiedCount())
  }

  @Test
  public fun acquire_returnsTheExistingSlotForTheSameOwner(): Unit {
    // Arrange
    val tracker = OemSlotTracker(maxSlots = 2)
    tracker.acquire(ownerId = "pixel-9")

    // Act
    val actual = tracker.acquire(ownerId = " Pixel-9 ")

    // Assert
    assertEquals(expected = 0, actual = actual)
    assertEquals(expected = 1, actual = tracker.occupiedCount())
  }

  @Test
  public fun acquire_returnsNullWhenNoSlotsRemain(): Unit {
    // Arrange
    val tracker = OemSlotTracker(maxSlots = 1)
    tracker.acquire(ownerId = "pixel-9")

    // Act
    val actual = tracker.acquire(ownerId = "iphone-16")

    // Assert
    assertEquals(expected = null, actual = actual)
  }

  @Test
  public fun release_freesSlotsForReuse(): Unit {
    // Arrange
    val tracker = OemSlotTracker(maxSlots = 2)
    tracker.acquire(ownerId = "pixel-9")
    tracker.acquire(ownerId = "iphone-16")
    tracker.release(ownerId = "pixel-9")

    // Act
    val actual = tracker.acquire(ownerId = "galaxy-s25")

    // Assert
    assertEquals(expected = 0, actual = actual)
    assertEquals(expected = 2, actual = tracker.occupiedCount())
  }

  @Test
  public fun release_ignoresOwnersThatWereNotAllocated(): Unit {
    // Arrange
    val tracker = OemSlotTracker(maxSlots = 1)
    tracker.acquire(ownerId = "pixel-9")

    // Act
    tracker.release(ownerId = "iphone-16")
    val actual = tracker.occupiedCount()

    // Assert
    assertEquals(expected = 1, actual = actual)
  }

  @Test
  public fun release_rejectsBlankOwnerIds(): Unit {
    // Arrange
    val tracker = OemSlotTracker()

    // Act
    val error = assertFailsWith<IllegalArgumentException> { tracker.release(ownerId = "   ") }

    // Assert
    assertEquals(expected = "OemSlotTracker ownerId must not be blank.", actual = error.message)
  }

  @Test
  public fun init_rejectsNonPositiveSlotCounts(): Unit {
    // Arrange
    val expectedMessage = "OemSlotTracker maxSlots must be greater than 0."

    // Act
    val error = assertFailsWith<IllegalArgumentException> { OemSlotTracker(maxSlots = 0) }

    // Assert
    assertEquals(expected = expectedMessage, actual = error.message)
  }
}
