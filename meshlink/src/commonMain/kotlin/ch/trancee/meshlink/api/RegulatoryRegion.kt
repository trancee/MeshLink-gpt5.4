package ch.trancee.meshlink.api

/**
 * Region-specific radio policy guardrails.
 *
 * The current model keeps only the limits needed by configuration normalization, not a full catalog
 * of BLE regulations.
 */
public enum class RegulatoryRegion(
  private val minimumAdvertisementIntervalMs: Int,
  private val maximumScanDutyCyclePercent: Int,
) {
  WORLDWIDE(minimumAdvertisementIntervalMs = 100, maximumScanDutyCyclePercent = 100),
  US(minimumAdvertisementIntervalMs = 100, maximumScanDutyCyclePercent = 100),
  EU(minimumAdvertisementIntervalMs = 300, maximumScanDutyCyclePercent = 70);

  /** Clamps advertising cadence so the resulting value satisfies the region policy. */
  public fun clampAdvertisementIntervalMs(intervalMs: Int): Int {
    return intervalMs.coerceAtLeast(minimumValue = minimumAdvertisementIntervalMs)
  }

  /** Clamps scan duty cycle to the region's maximum allowed aggressiveness. */
  public fun clampScanDutyCyclePercent(percent: Int): Int {
    return percent.coerceIn(minimumValue = 0, maximumValue = maximumScanDutyCyclePercent)
  }
}
