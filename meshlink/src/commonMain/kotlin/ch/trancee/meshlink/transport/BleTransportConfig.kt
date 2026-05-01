package ch.trancee.meshlink.transport

public data class BleTransportConfig(
  public val scanIntervalMillis: Int,
  public val scanWindowMillis: Int,
  public val connectionTimeoutMillis: Int,
  public val advertisementIntervalMillis: Int,
) {
  init {
    require(scanIntervalMillis > 0) {
      "BleTransportConfig scanIntervalMillis must be greater than 0."
    }
    require(scanWindowMillis > 0) { "BleTransportConfig scanWindowMillis must be greater than 0." }
    require(scanWindowMillis <= scanIntervalMillis) {
      "BleTransportConfig scanWindowMillis must be less than or equal to scanIntervalMillis."
    }
    require(connectionTimeoutMillis > 0) {
      "BleTransportConfig connectionTimeoutMillis must be greater than 0."
    }
    require(advertisementIntervalMillis > 0) {
      "BleTransportConfig advertisementIntervalMillis must be greater than 0."
    }
  }

  public companion object {
    public fun default(): BleTransportConfig {
      return BleTransportConfig(
        scanIntervalMillis = 5_000,
        scanWindowMillis = 2_500,
        connectionTimeoutMillis = 15_000,
        advertisementIntervalMillis = 300,
      )
    }
  }
}
