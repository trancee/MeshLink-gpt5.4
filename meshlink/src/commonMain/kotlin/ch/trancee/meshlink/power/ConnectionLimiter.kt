package ch.trancee.meshlink.power

/** Enforces tier-specific connection-count limits. */
public object ConnectionLimiter {
  public fun canAcceptConnection(
    currentConnectionCount: Int,
    tier: PowerTier,
    config: PowerConfig,
  ): Boolean {
    require(currentConnectionCount >= 0) {
      "ConnectionLimiter currentConnectionCount must be greater than or equal to 0."
    }

    return currentConnectionCount <
      BleConnectionParameterPolicy.profileFor(tier = tier, config = config).maxConnections
  }
}
