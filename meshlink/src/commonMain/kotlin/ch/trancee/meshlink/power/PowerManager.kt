package ch.trancee.meshlink.power

/** High-level coordinator that translates battery state into connection policy. */
public class PowerManager(
  private val batteryMonitor: BatteryMonitor,
  private val config: PowerConfig,
  private val modeEngine: PowerModeEngine = PowerModeEngine(),
) {
  private var currentTier: PowerTier? = null

  /** Produces a power decision for the current connection set. */
  public fun evaluate(connections: List<ManagedConnection>): PowerDecision {
    val batteryPercent: Int = batteryMonitor.batteryPercent()
    val activeTier: PowerTier =
      modeEngine.evaluate(
        currentTier = currentTier,
        batteryPercent = batteryPercent,
        config = config,
      )
    currentTier = activeTier
    val profile: PowerProfile =
      BleConnectionParameterPolicy.profileFor(tier = activeTier, config = config)
    val canAcceptConnection: Boolean =
      ConnectionLimiter.canAcceptConnection(
        currentConnectionCount = connections.size,
        tier = activeTier,
        config = config,
      )
    val shedConnections: List<ManagedConnection> =
      TieredShedder.shed(connections = connections, targetConnectionCount = profile.maxConnections)
    return PowerDecision(
      tier = activeTier,
      profile = profile,
      canAcceptConnection = canAcceptConnection,
      shedConnections = shedConnections,
    )
  }
}

/** Decision returned by [PowerManager] for the current evaluation cycle. */
public data class PowerDecision(
  public val tier: PowerTier,
  public val profile: PowerProfile,
  public val canAcceptConnection: Boolean,
  public val shedConnections: List<ManagedConnection>,
)
