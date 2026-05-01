package ch.trancee.meshlink.power

public class FixedBatteryMonitor(initialBatteryPercent: Int) : BatteryMonitor {
  private var currentBatteryPercent: Int =
    validateBatteryPercent(batteryPercent = initialBatteryPercent)

  override fun batteryPercent(): Int {
    return currentBatteryPercent
  }

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
