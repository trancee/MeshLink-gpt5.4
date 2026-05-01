package ch.trancee.meshlink.power

/** Mutable battery monitor used for tests and deterministic host-side simulations. */
public class FixedBatteryMonitor(initialBatteryPercent: Int) : BatteryMonitor {
  private var currentBatteryPercent: Int =
    validateBatteryPercent(batteryPercent = initialBatteryPercent)

  override fun batteryPercent(): Int {
    return currentBatteryPercent
  }

  /** Updates the reported battery level. */
  public fun update(batteryPercent: Int): Unit {
    currentBatteryPercent = validateBatteryPercent(batteryPercent = batteryPercent)
  }

  private fun validateBatteryPercent(batteryPercent: Int): Int {
    require(batteryPercent in 0..100) {
      "FixedBatteryMonitor batteryPercent must be between 0 and 100."
    }
    return batteryPercent
  }
}
