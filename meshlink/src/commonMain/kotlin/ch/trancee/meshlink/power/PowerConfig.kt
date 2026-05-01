package ch.trancee.meshlink.power

public data class PowerConfig(
    public val highTierThresholdPercent: Int,
    public val normalTierThresholdPercent: Int,
    public val batteryPollIntervalMillis: Long,
    public val highProfile: PowerProfile,
    public val normalProfile: PowerProfile,
    public val lowProfile: PowerProfile,
) {
    init {
        require(highTierThresholdPercent in 0..100) {
            "PowerConfig highTierThresholdPercent must be between 0 and 100."
        }
        require(normalTierThresholdPercent in 0..100) {
            "PowerConfig normalTierThresholdPercent must be between 0 and 100."
        }
        require(highTierThresholdPercent > normalTierThresholdPercent) {
            "PowerConfig highTierThresholdPercent must be greater than normalTierThresholdPercent."
        }
        require(batteryPollIntervalMillis > 0) {
            "PowerConfig batteryPollIntervalMillis must be greater than 0."
        }
        require(lowProfile.scanDutyCyclePercent <= LOW_TIER_MAX_SCAN_DUTY_PERCENT) {
            "PowerConfig lowProfile.scanDutyCyclePercent must not exceed 5."
        }
    }

    public companion object {
        private const val LOW_TIER_MAX_SCAN_DUTY_PERCENT: Int = 5

        public fun default(): PowerConfig {
            return PowerConfig(
                highTierThresholdPercent = 80,
                normalTierThresholdPercent = 30,
                batteryPollIntervalMillis = 60_000L,
                highProfile = PowerProfile(scanDutyCyclePercent = 100, connectionIntervalMillis = 100, maxConnections = 8),
                normalProfile = PowerProfile(scanDutyCyclePercent = 50, connectionIntervalMillis = 250, maxConnections = 4),
                lowProfile = PowerProfile(scanDutyCyclePercent = 5, connectionIntervalMillis = 500, maxConnections = 2),
            )
        }
    }
}
