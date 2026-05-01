package ch.trancee.meshlink.power

/** Source of current battery percentage for power-management decisions. */
public interface BatteryMonitor {
  public fun batteryPercent(): Int
}
