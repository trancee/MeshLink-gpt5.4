package ch.trancee.meshlink.transport

public class OemL2capProbeCache(private val maxEntries: Int = DEFAULT_MAX_ENTRIES) {
  private val entries: MutableList<OemL2capProbeCacheEntry> = mutableListOf()

  init {
    require(maxEntries > 0) { "OemL2capProbeCache maxEntries must be greater than 0." }
  }

  public fun get(deviceModel: String): Boolean? {
    val normalizedDeviceModel: String = deviceModel.normalizeDeviceModel()
    val existingIndex: Int =
      entries.indexOfFirst { entry -> entry.deviceModel == normalizedDeviceModel }
    if (existingIndex < 0) {
      return null
    }

    val existingEntry: OemL2capProbeCacheEntry = entries.removeAt(index = existingIndex)
    entries += existingEntry
    return existingEntry.supportsL2cap
  }

  public fun record(deviceModel: String, supportsL2cap: Boolean): Unit {
    val normalizedDeviceModel: String = deviceModel.normalizeDeviceModel()
    val existingIndex: Int =
      entries.indexOfFirst { entry -> entry.deviceModel == normalizedDeviceModel }
    if (existingIndex >= 0) {
      entries.removeAt(index = existingIndex)
    } else if (entries.size == maxEntries) {
      entries.removeAt(index = 0)
    }

    entries +=
      OemL2capProbeCacheEntry(deviceModel = normalizedDeviceModel, supportsL2cap = supportsL2cap)
  }

  public fun clear(): Unit {
    entries.clear()
  }

  public companion object {
    public const val DEFAULT_MAX_ENTRIES: Int = 64
  }
}

private data class OemL2capProbeCacheEntry(val deviceModel: String, val supportsL2cap: Boolean)

private fun String.normalizeDeviceModel(): String {
  val normalizedValue: String = trim().lowercase()
  require(normalizedValue.isNotEmpty()) { "OemL2capProbeCache deviceModel must not be blank." }
  return normalizedValue
}
