package ch.trancee.meshlink.api

public enum class RegulatoryRegion(
    private val minimumAdvertisementIntervalMs: Int,
    private val maximumScanDutyCyclePercent: Int,
) {
    WORLDWIDE(
        minimumAdvertisementIntervalMs = 100,
        maximumScanDutyCyclePercent = 100,
    ),
    US(
        minimumAdvertisementIntervalMs = 100,
        maximumScanDutyCyclePercent = 100,
    ),
    EU(
        minimumAdvertisementIntervalMs = 300,
        maximumScanDutyCyclePercent = 70,
    ),
    ;

    public fun clampAdvertisementIntervalMs(intervalMs: Int): Int {
        return intervalMs.coerceAtLeast(minimumValue = minimumAdvertisementIntervalMs)
    }

    public fun clampScanDutyCyclePercent(percent: Int): Int {
        return percent.coerceIn(minimumValue = 0, maximumValue = maximumScanDutyCyclePercent)
    }
}
