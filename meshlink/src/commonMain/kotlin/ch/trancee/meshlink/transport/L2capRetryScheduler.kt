package ch.trancee.meshlink.transport

import kotlin.math.roundToLong

public class L2capRetryScheduler(
  private val initialDelayMillis: Long = DEFAULT_INITIAL_DELAY_MILLIS,
  private val maxDelayMillis: Long = DEFAULT_MAX_DELAY_MILLIS,
  private val jitterRatio: Double = DEFAULT_JITTER_RATIO,
  private val randomUnitInterval: () -> Double = { 0.5 },
) {
  init {
    require(initialDelayMillis > 0) {
      "L2capRetryScheduler initialDelayMillis must be greater than 0."
    }
    require(maxDelayMillis >= initialDelayMillis) {
      "L2capRetryScheduler maxDelayMillis must be greater than or equal to initialDelayMillis."
    }
    require(jitterRatio in 0.0..1.0) {
      "L2capRetryScheduler jitterRatio must be between 0.0 and 1.0."
    }
  }

  public fun delayMillisForAttempt(attempt: Int): Long {
    require(attempt >= 0) { "L2capRetryScheduler attempt must be greater than or equal to 0." }

    var baseDelayMillis: Long = initialDelayMillis
    repeat(times = attempt) {
      baseDelayMillis =
        when {
          baseDelayMillis >= maxDelayMillis -> maxDelayMillis
          baseDelayMillis > maxDelayMillis / 2 -> maxDelayMillis
          else -> baseDelayMillis * 2
        }
    }

    if (jitterRatio == 0.0) {
      return baseDelayMillis
    }

    val randomValue: Double = randomUnitInterval()
    require(randomValue in 0.0..1.0) {
      "L2capRetryScheduler randomUnitInterval must return a value between 0.0 and 1.0."
    }

    val jitterWindowMillis: Double = baseDelayMillis * jitterRatio
    val jitterMultiplier: Double = (randomValue * 2.0) - 1.0
    val jitterOffsetMillis: Long = (jitterWindowMillis * jitterMultiplier).roundToLong()
    return (baseDelayMillis + jitterOffsetMillis).coerceIn(
      minimumValue = 0L,
      maximumValue = maxDelayMillis,
    )
  }

  public companion object {
    public const val DEFAULT_INITIAL_DELAY_MILLIS: Long = 250L
    public const val DEFAULT_MAX_DELAY_MILLIS: Long = 8_000L
    public const val DEFAULT_JITTER_RATIO: Double = 0.2
  }
}
