package ch.trancee.meshlink.routing

import ch.trancee.meshlink.api.PeerIdHex

/** Candidate route advertisement ready for routing-table evaluation. */
public data class RoutingUpdate(
  public val destinationPeerId: PeerIdHex,
  public val nextHopPeerId: PeerIdHex,
  public val metric: Int,
  public val sequenceNumber: Int,
  public val expiresAtEpochMillis: Long,
) {
  init {
    require(metric >= 0) { "RoutingUpdate metric must be greater than or equal to 0." }
    require(sequenceNumber >= 0) {
      "RoutingUpdate sequenceNumber must be greater than or equal to 0."
    }
    require(expiresAtEpochMillis >= 0) {
      "RoutingUpdate expiresAtEpochMillis must be greater than or equal to 0."
    }
  }
}

/**
 * Applies routing updates to the local routing table while enforcing hop-limit and feasibility
 * constraints.
 */
public class RoutingEngine(
  public val config: RoutingConfig,
  private val routingTable: RoutingTable = RoutingTable(),
  internal val routeCoordinator: RouteCoordinator = RouteCoordinator(),
) {
  /**
   * Processes a single route advertisement.
   *
   * Withdrawals are handled before feasibility checks because an infinite metric is a terminal
   * signal telling the node to forget the next hop immediately.
   */
  public fun processUpdate(update: RoutingUpdate): Boolean {
    if (update.metric >= INFINITE_METRIC) {
      routingTable.withdraw(
        destinationPeerId = update.destinationPeerId,
        nextHopPeerId = update.nextHopPeerId,
      )
      if (routingTable.bestRoute(destinationPeerId = update.destinationPeerId) == null) {
        routeCoordinator.requestSequenceNumber(destinationPeerId = update.destinationPeerId)
      }
      return true
    }

    if (update.metric > config.hopLimit) {
      return false
    }

    if (
      !routeCoordinator.isFeasible(
        destinationPeerId = update.destinationPeerId,
        sequenceNumber = update.sequenceNumber,
        metric = update.metric,
      )
    ) {
      if (routingTable.bestRoute(destinationPeerId = update.destinationPeerId) == null) {
        routeCoordinator.requestSequenceNumber(destinationPeerId = update.destinationPeerId)
      }
      return false
    }

    routeCoordinator.recordAcceptedRoute(
      destinationPeerId = update.destinationPeerId,
      sequenceNumber = update.sequenceNumber,
      metric = update.metric,
    )
    routingTable.install(
      route =
        RouteEntry(
          destinationPeerId = update.destinationPeerId,
          nextHopPeerId = update.nextHopPeerId,
          metric = update.metric,
          sequenceNumber = update.sequenceNumber,
          expiresAtEpochMillis = update.expiresAtEpochMillis,
        )
    )
    return true
  }

  /** Returns the preferred next hop for the destination, if one exists. */
  public fun nextHopFor(destinationPeerId: PeerIdHex): PeerIdHex? {
    return routingTable.bestRoute(destinationPeerId = destinationPeerId)?.nextHopPeerId
  }

  /** Returns every known route candidate for the destination. */
  public fun routesFor(destinationPeerId: PeerIdHex): List<RouteEntry> {
    return routingTable.routesFor(destinationPeerId = destinationPeerId)
  }

  /** Returns every destination currently represented in the routing table. */
  public fun destinations(): Set<PeerIdHex> {
    return routingTable.destinations()
  }

  public companion object {
    public const val INFINITE_METRIC: Int = Int.MAX_VALUE
  }
}
