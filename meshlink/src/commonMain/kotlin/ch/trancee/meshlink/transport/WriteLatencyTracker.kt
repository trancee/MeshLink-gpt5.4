package ch.trancee.meshlink.transport

/** Rolling tracker of recent write latencies. */
public class WriteLatencyTracker(private val windowSize: Int = DEFAULT_WINDOW_SIZE) {
  private val samples: MutableList<Long> = mutableListOf()

  init {
    require(windowSize > 0) { "WriteLatencyTracker windowSize must be greater than 0." }
  }

  /** Adds a new latency sample to the rolling window. */
  public fun record(durationMillis: Long): Unit {
    require(durationMillis >= 0) {
      "WriteLatencyTracker durationMillis must be greater than or equal to 0."
    }

    if (samples.size == windowSize) {
      samples.removeAt(index = 0)
    }
    samples += durationMillis
  }

  /** Returns the current aggregate latency snapshot. */
  public fun snapshot(): WriteLatencySnapshot {
    if (samples.isEmpty()) {
      return WriteLatencySnapshot(
        sampleCount = 0,
        minimumMillis = 0,
        maximumMillis = 0,
        averageMillis = 0.0,
      )
    }

    val minimumMillis: Long = samples.min()
    val maximumMillis: Long = samples.max()
    val averageMillis: Double = samples.average()
    return WriteLatencySnapshot(
      sampleCount = samples.size,
      minimumMillis = minimumMillis,
      maximumMillis = maximumMillis,
      averageMillis = averageMillis,
    )
  }

  public companion object {
    public const val DEFAULT_WINDOW_SIZE: Int = 16
  }
}

/** Aggregate write-latency statistics over the current window. */
public data class WriteLatencySnapshot(
  public val sampleCount: Int,
  public val minimumMillis: Long,
  public val maximumMillis: Long,
  public val averageMillis: Double,
)
