package ch.trancee.meshlink.power

public object BleConnectionParameterPolicy {
    public fun profileFor(
        tier: PowerTier,
        config: PowerConfig,
    ): PowerProfile {
        return when (tier) {
            PowerTier.HIGH -> config.highProfile
            PowerTier.NORMAL -> config.normalProfile
            PowerTier.LOW -> config.lowProfile
        }
    }
}
