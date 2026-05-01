package ch.trancee.meshlink.power

public data class PowerProfile(
  public val scanDutyCyclePercent: Int,
  public val connectionIntervalMillis: Int,
  public val maxConnections: Int,
) {
  init {
    require(scanDutyCyclePercent in 0..100) {
      "PowerProfile scanDutyCyclePercent must be between 0 and 100."
    }
    require(connectionIntervalMillis > 0) {
      "PowerProfile connectionIntervalMillis must be greater than 0."
    }
    require(maxConnections >= 0) {
      "PowerProfile maxConnections must be greater than or equal to 0."
    }
  }
}
