package ch.trancee.meshlink.routing

import ch.trancee.meshlink.api.PeerIdHex

public data class RouteEntry(
  public val destinationPeerId: PeerIdHex,
  public val nextHopPeerId: PeerIdHex,
  public val metric: Int,
  public val sequenceNumber: Int,
  public val expiresAtEpochMillis: Long,
) {
  init {
    require(metric >= 0) { "RouteEntry metric must be greater than or equal to 0." }
    require(sequenceNumber >= 0) { "RouteEntry sequenceNumber must be greater than or equal to 0." }
    require(expiresAtEpochMillis >= 0) {
      "RouteEntry expiresAtEpochMillis must be greater than or equal to 0."
    }
  }
}

public class RoutingTable {
  private val routesByDestination: MutableMap<String, MutableList<RouteEntry>> = mutableMapOf()

  public fun install(route: RouteEntry): Unit {
    val routesForDestination: MutableList<RouteEntry> =
      routesByDestination.getOrPut(route.destinationPeerId.value) { mutableListOf() }

    val existingIndex: Int =
      routesForDestination.indexOfFirst { existingRoute ->
        existingRoute.nextHopPeerId == route.nextHopPeerId
      }
    if (existingIndex >= 0) {
      routesForDestination.removeAt(existingIndex)
    }
    routesForDestination += route
    routesForDestination.sortWith(routeOrdering)
  }

  public fun bestRoute(destinationPeerId: PeerIdHex): RouteEntry? {
    val routesForDestination: List<RouteEntry> =
      routesByDestination[destinationPeerId.value] ?: return null
    return routesForDestination.sortedWith(routeOrdering).first()
  }

  public fun routesFor(destinationPeerId: PeerIdHex): List<RouteEntry> {
    val routesForDestination: List<RouteEntry> =
      routesByDestination[destinationPeerId.value] ?: return emptyList()
    return routesForDestination.sortedWith(routeOrdering)
  }

  public fun withdraw(destinationPeerId: PeerIdHex, nextHopPeerId: PeerIdHex): Unit {
    val routesForDestination: MutableList<RouteEntry> =
      routesByDestination[destinationPeerId.value] ?: return
    routesForDestination.removeAll { route -> route.nextHopPeerId == nextHopPeerId }
    if (routesForDestination.isEmpty()) {
      routesByDestination.remove(destinationPeerId.value)
    }
  }

  public fun destinations(): Set<PeerIdHex> {
    return routesByDestination.keys.mapTo(destination = linkedSetOf()) { destinationPeerId ->
      PeerIdHex(value = destinationPeerId)
    }
  }

  public companion object {
    private val routeOrdering: Comparator<RouteEntry> =
      compareBy<RouteEntry> { route -> route.metric }
        .thenByDescending { route -> route.sequenceNumber }
        .thenBy { route -> route.nextHopPeerId.value }
  }
}
