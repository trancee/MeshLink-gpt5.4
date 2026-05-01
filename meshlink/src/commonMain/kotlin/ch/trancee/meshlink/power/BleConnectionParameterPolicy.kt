package ch.trancee.meshlink.power

/** Maps a power tier to the corresponding BLE operating profile. */
public object BleConnectionParameterPolicy {
  public fun profileFor(tier: PowerTier, config: PowerConfig): PowerProfile {
    return when (tier) {
      PowerTier.HIGH -> config.highProfile
      PowerTier.NORMAL -> config.normalProfile
      PowerTier.LOW -> config.lowProfile
    }
  }
}
