package ch.trancee.meshlink.routing

import ch.trancee.meshlink.api.PeerIdHex

public data class RoutingUpdate(
    public val destinationPeerId: PeerIdHex,
    public val nextHopPeerId: PeerIdHex,
    public val metric: Int,
    public val sequenceNumber: Int,
    public val expiresAtEpochMillis: Long,
) {
    init {
        require(metric >= 0) {
            "RoutingUpdate metric must be greater than or equal to 0."
        }
        require(sequenceNumber >= 0) {
            "RoutingUpdate sequenceNumber must be greater than or equal to 0."
        }
        require(expiresAtEpochMillis >= 0) {
            "RoutingUpdate expiresAtEpochMillis must be greater than or equal to 0."
        }
    }
}

public class RoutingEngine(
    public val config: RoutingConfig,
    private val routingTable: RoutingTable = RoutingTable(),
    private val routeCoordinator: RouteCoordinator = RouteCoordinator(),
) {
    public fun processUpdate(update: RoutingUpdate): Boolean {
        if (update.metric >= INFINITE_METRIC) {
            routingTable.withdraw(
                destinationPeerId = update.destinationPeerId,
                nextHopPeerId = update.nextHopPeerId,
            )
            if (routingTable.bestRoute(destinationPeerId = update.destinationPeerId) == null) {
                routeCoordinator.withdraw(destinationPeerId = update.destinationPeerId)
            }
            return true
        }

        if (update.metric > config.hopLimit) {
            return false
        }

        if (!routeCoordinator.isFeasible(
                destinationPeerId = update.destinationPeerId,
                sequenceNumber = update.sequenceNumber,
                metric = update.metric,
            )
        ) {
            return false
        }

        routeCoordinator.recordAcceptedRoute(
            destinationPeerId = update.destinationPeerId,
            sequenceNumber = update.sequenceNumber,
            metric = update.metric,
        )
        routingTable.install(
            route = RouteEntry(
                destinationPeerId = update.destinationPeerId,
                nextHopPeerId = update.nextHopPeerId,
                metric = update.metric,
                sequenceNumber = update.sequenceNumber,
                expiresAtEpochMillis = update.expiresAtEpochMillis,
            ),
        )
        return true
    }

    public fun nextHopFor(destinationPeerId: PeerIdHex): PeerIdHex? {
        return routingTable.bestRoute(destinationPeerId = destinationPeerId)?.nextHopPeerId
    }

    public fun routesFor(destinationPeerId: PeerIdHex): List<RouteEntry> {
        return routingTable.routesFor(destinationPeerId = destinationPeerId)
    }

    public fun destinations(): Set<PeerIdHex> {
        return routingTable.destinations()
    }

    public companion object {
        public const val INFINITE_METRIC: Int = Int.MAX_VALUE
    }
}
