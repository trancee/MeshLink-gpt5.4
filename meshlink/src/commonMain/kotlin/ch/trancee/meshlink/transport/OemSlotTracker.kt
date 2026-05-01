package ch.trancee.meshlink.transport

/** Tracks a small pool of OEM-specific slots by owner identifier. */
public class OemSlotTracker(private val maxSlots: Int = DEFAULT_MAX_SLOTS) {
  private val allocations: MutableList<OemSlotAllocation> = mutableListOf()

  init {
    require(maxSlots > 0) { "OemSlotTracker maxSlots must be greater than 0." }
  }

  /** Acquires or returns the existing slot for the owner. */
  public fun acquire(ownerId: String): Int? {
    val normalizedOwnerId: String = ownerId.normalizeOwnerId()
    allocations
      .firstOrNull { allocation -> allocation.ownerId == normalizedOwnerId }
      ?.let { allocation ->
        return allocation.slot
      }

    val occupiedSlots: Set<Int> = allocations.map { allocation -> allocation.slot }.toSet()
    val availableSlot: Int =
      (0 until maxSlots).firstOrNull { slot -> slot !in occupiedSlots } ?: return null
    allocations += OemSlotAllocation(ownerId = normalizedOwnerId, slot = availableSlot)
    return availableSlot
  }

  /** Releases any slot held by the owner. */
  public fun release(ownerId: String): Unit {
    val normalizedOwnerId: String = ownerId.normalizeOwnerId()
    val existingIndex: Int =
      allocations.indexOfFirst { allocation -> allocation.ownerId == normalizedOwnerId }
    if (existingIndex >= 0) {
      allocations.removeAt(index = existingIndex)
    }
  }

  public fun occupiedCount(): Int {
    return allocations.size
  }

  public companion object {
    public const val DEFAULT_MAX_SLOTS: Int = 4
  }
}

private data class OemSlotAllocation(val ownerId: String, val slot: Int)

private fun String.normalizeOwnerId(): String {
  val normalizedValue: String = trim().lowercase()
  require(normalizedValue.isNotEmpty()) { "OemSlotTracker ownerId must not be blank." }
  return normalizedValue
}
