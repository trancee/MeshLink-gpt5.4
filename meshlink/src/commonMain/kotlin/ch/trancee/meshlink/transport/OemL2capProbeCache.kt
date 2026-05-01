package ch.trancee.meshlink.transport

/** LRU-style cache of observed L2CAP support by device model. */
public class OemL2capProbeCache(private val maxEntries: Int = DEFAULT_MAX_ENTRIES) {
  private val entries: MutableList<OemL2capProbeCacheEntry> = mutableListOf()

  init {
    require(maxEntries > 0) { "OemL2capProbeCache maxEntries must be greater than 0." }
  }

  /** Returns the cached probe result for the device model, if known. */
  public fun get(deviceModel: String): Boolean? {
    return touch(deviceModel = deviceModel)?.supportsL2cap
  }

  /** Records whether the device model supports L2CAP. */
  public fun record(deviceModel: String, supportsL2cap: Boolean): Unit {
    recordProbe(
      deviceModel = deviceModel,
      supportsL2cap = supportsL2cap,
      observedAtEpochMillis = 0L,
    )
  }

  /** Records a capability observation at a specific instant for freshness-aware fallback logic. */
  internal fun recordProbe(
    deviceModel: String,
    supportsL2cap: Boolean,
    observedAtEpochMillis: Long,
  ): Unit {
    require(observedAtEpochMillis >= 0L) {
      "OemL2capProbeCache observedAtEpochMillis must be greater than or equal to 0."
    }
    val normalizedDeviceModel: String = deviceModel.normalizeDeviceModel()
    val existingIndex: Int =
      entries.indexOfFirst { entry -> entry.deviceModel == normalizedDeviceModel }
    if (existingIndex >= 0) {
      entries.removeAt(index = existingIndex)
    } else if (entries.size == maxEntries) {
      entries.removeAt(index = 0)
    }

    entries +=
      OemL2capProbeCacheEntry(
        deviceModel = normalizedDeviceModel,
        supportsL2cap = supportsL2cap,
        observedAtEpochMillis = observedAtEpochMillis,
      )
  }

  /** Returns the cached capability and whether it has gone stale. */
  internal fun probe(
    deviceModel: String,
    nowEpochMillis: Long,
    maxAgeMillis: Long = DEFAULT_MAX_AGE_MILLIS,
  ): OemL2capProbeResult? {
    require(nowEpochMillis >= 0L) {
      "OemL2capProbeCache nowEpochMillis must be greater than or equal to 0."
    }
    require(maxAgeMillis >= 0L) {
      "OemL2capProbeCache maxAgeMillis must be greater than or equal to 0."
    }
    val entry: OemL2capProbeCacheEntry = touch(deviceModel = deviceModel) ?: return null
    return OemL2capProbeResult(
      supportsL2cap = entry.supportsL2cap,
      isStale = nowEpochMillis - entry.observedAtEpochMillis > maxAgeMillis,
    )
  }

  public fun clear(): Unit {
    entries.clear()
  }

  private fun touch(deviceModel: String): OemL2capProbeCacheEntry? {
    val normalizedDeviceModel: String = deviceModel.normalizeDeviceModel()
    val existingIndex: Int =
      entries.indexOfFirst { entry -> entry.deviceModel == normalizedDeviceModel }
    if (existingIndex < 0) {
      return null
    }

    val existingEntry: OemL2capProbeCacheEntry = entries.removeAt(index = existingIndex)
    entries += existingEntry
    return existingEntry
  }

  public companion object {
    public const val DEFAULT_MAX_ENTRIES: Int = 64
    internal const val DEFAULT_MAX_AGE_MILLIS: Long = 86_400_000L
  }
}

internal data class OemL2capProbeResult(
  internal val supportsL2cap: Boolean,
  internal val isStale: Boolean,
)

private data class OemL2capProbeCacheEntry(
  val deviceModel: String,
  val supportsL2cap: Boolean,
  val observedAtEpochMillis: Long,
)

private fun String.normalizeDeviceModel(): String {
  val normalizedValue: String = trim().lowercase()
  require(normalizedValue.isNotEmpty()) { "OemL2capProbeCache deviceModel must not be blank." }
  return normalizedValue
}
