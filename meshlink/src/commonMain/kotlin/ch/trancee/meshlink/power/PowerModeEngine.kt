package ch.trancee.meshlink.power

/**
 * Maps battery percentage to a power tier while applying hysteresis to avoid rapid oscillation
 * around thresholds.
 */
public class PowerModeEngine(private val hysteresisPercent: Int = DEFAULT_HYSTERESIS_PERCENT) {
  init {
    require(hysteresisPercent >= 0) {
      "PowerModeEngine hysteresisPercent must be greater than or equal to 0."
    }
  }

  /** Evaluates the desired power tier for the current battery reading. */
  public fun evaluate(
    currentTier: PowerTier?,
    batteryPercent: Int,
    config: PowerConfig,
  ): PowerTier {
    require(batteryPercent in 0..100) {
      "PowerModeEngine batteryPercent must be between 0 and 100."
    }

    if (currentTier == null) {
      return tierForBatteryPercent(batteryPercent = batteryPercent, config = config)
    }

    return when (currentTier) {
      PowerTier.HIGH ->
        if (batteryPercent < config.highTierThresholdPercent - hysteresisPercent) {
          tierForBatteryPercent(batteryPercent = batteryPercent, config = config)
        } else {
          PowerTier.HIGH
        }
      PowerTier.NORMAL ->
        when {
          batteryPercent >= config.highTierThresholdPercent + hysteresisPercent -> PowerTier.HIGH
          batteryPercent < config.normalTierThresholdPercent - hysteresisPercent -> PowerTier.LOW
          else -> PowerTier.NORMAL
        }
      PowerTier.LOW ->
        if (batteryPercent >= config.normalTierThresholdPercent + hysteresisPercent) {
          tierForBatteryPercent(batteryPercent = batteryPercent, config = config)
        } else {
          PowerTier.LOW
        }
    }
  }

  private fun tierForBatteryPercent(batteryPercent: Int, config: PowerConfig): PowerTier {
    return when {
      batteryPercent >= config.highTierThresholdPercent -> PowerTier.HIGH
      batteryPercent >= config.normalTierThresholdPercent -> PowerTier.NORMAL
      else -> PowerTier.LOW
    }
  }

  public companion object {
    public const val DEFAULT_HYSTERESIS_PERCENT: Int = 5
  }
}
