package ch.trancee.meshlink.routing

import ch.trancee.meshlink.api.PeerIdHex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class RoutingEngineTest {
    @Test
    public fun processUpdate_installsReachableRoutesAndSelectsTheBestNextHop(): Unit {
        // Arrange
        val destinationPeerId = PeerIdHex(value = "00112233")
        val betterNextHop = PeerIdHex(value = "44556677")
        val fallbackNextHop = PeerIdHex(value = "8899aabb")
        val engine = RoutingEngine(config = RoutingConfig.default())

        // Act
        val fallbackAccepted = engine.processUpdate(
            update = routingUpdate(
                destinationPeerId = destinationPeerId,
                nextHopPeerId = fallbackNextHop,
                metric = 3,
                sequenceNumber = 1,
            ),
        )
        val betterAccepted = engine.processUpdate(
            update = routingUpdate(
                destinationPeerId = destinationPeerId,
                nextHopPeerId = betterNextHop,
                metric = 1,
                sequenceNumber = 2,
            ),
        )
        val actualNextHop = engine.nextHopFor(destinationPeerId = destinationPeerId)

        // Assert
        assertEquals(expected = true, actual = fallbackAccepted)
        assertEquals(expected = true, actual = betterAccepted)
        assertEquals(expected = betterNextHop, actual = actualNextHop)
    }

    @Test
    public fun processUpdate_rejectsRoutesBeyondTheConfiguredHopLimit(): Unit {
        // Arrange
        val destinationPeerId = PeerIdHex(value = "00112233")
        val engine = RoutingEngine(
            config = RoutingConfig(routeExpiryMillis = 1L, peerTimeoutMillis = 1L, hopLimit = 2),
        )

        // Act
        val actual = engine.processUpdate(
            update = routingUpdate(
                destinationPeerId = destinationPeerId,
                nextHopPeerId = PeerIdHex(value = "44556677"),
                metric = 3,
            ),
        )

        // Assert
        assertEquals(expected = false, actual = actual)
        assertEquals(expected = null, actual = engine.nextHopFor(destinationPeerId = destinationPeerId))
    }

    @Test
    public fun processUpdate_rejectsInfeasibleRoutesFromTheCoordinator(): Unit {
        // Arrange
        val destinationPeerId = PeerIdHex(value = "00112233")
        val engine = RoutingEngine(config = RoutingConfig.default())
        engine.processUpdate(
            update = routingUpdate(
                destinationPeerId = destinationPeerId,
                nextHopPeerId = PeerIdHex(value = "44556677"),
                metric = 1,
                sequenceNumber = 5,
            ),
        )

        // Act
        val actual = engine.processUpdate(
            update = routingUpdate(
                destinationPeerId = destinationPeerId,
                nextHopPeerId = PeerIdHex(value = "8899aabb"),
                metric = 1,
                sequenceNumber = 4,
            ),
        )

        // Assert
        assertEquals(expected = false, actual = actual)
        assertEquals(expected = PeerIdHex(value = "44556677"), actual = engine.nextHopFor(destinationPeerId = destinationPeerId))
    }

    @Test
    public fun processUpdate_withdrawsRoutesAndPromotesAlternatives(): Unit {
        // Arrange
        val destinationPeerId = PeerIdHex(value = "00112233")
        val withdrawnNextHop = PeerIdHex(value = "44556677")
        val fallbackNextHop = PeerIdHex(value = "8899aabb")
        val engine = RoutingEngine(config = RoutingConfig.default())
        engine.processUpdate(
            update = routingUpdate(
                destinationPeerId = destinationPeerId,
                nextHopPeerId = withdrawnNextHop,
                metric = 1,
                sequenceNumber = 3,
            ),
        )
        engine.processUpdate(
            update = routingUpdate(
                destinationPeerId = destinationPeerId,
                nextHopPeerId = fallbackNextHop,
                metric = 2,
                sequenceNumber = 4,
            ),
        )

        // Act
        val actual = engine.processUpdate(
            update = routingUpdate(
                destinationPeerId = destinationPeerId,
                nextHopPeerId = withdrawnNextHop,
                metric = RoutingEngine.INFINITE_METRIC,
                sequenceNumber = 5,
            ),
        )

        // Assert
        assertEquals(expected = true, actual = actual)
        assertEquals(expected = fallbackNextHop, actual = engine.nextHopFor(destinationPeerId = destinationPeerId))
    }

    @Test
    public fun processUpdate_removesDestinationsWhenTheLastRouteIsWithdrawn(): Unit {
        // Arrange
        val destinationPeerId = PeerIdHex(value = "00112233")
        val nextHopPeerId = PeerIdHex(value = "44556677")
        val engine = RoutingEngine(config = RoutingConfig.default())
        engine.processUpdate(
            update = routingUpdate(
                destinationPeerId = destinationPeerId,
                nextHopPeerId = nextHopPeerId,
                metric = 1,
                sequenceNumber = 3,
            ),
        )

        // Act
        engine.processUpdate(
            update = routingUpdate(
                destinationPeerId = destinationPeerId,
                nextHopPeerId = nextHopPeerId,
                metric = RoutingEngine.INFINITE_METRIC,
                sequenceNumber = 4,
            ),
        )

        // Assert
        assertEquals(expected = null, actual = engine.nextHopFor(destinationPeerId = destinationPeerId))
        assertEquals(expected = emptySet(), actual = engine.destinations())
    }

    @Test
    public fun routesFor_exposesAllInstalledRoutesForADestination(): Unit {
        // Arrange
        val destinationPeerId = PeerIdHex(value = "00112233")
        val engine = RoutingEngine(config = RoutingConfig.default())
        val firstUpdate = routingUpdate(
            destinationPeerId = destinationPeerId,
            nextHopPeerId = PeerIdHex(value = "44556677"),
            metric = 1,
            sequenceNumber = 1,
        )
        val secondUpdate = routingUpdate(
            destinationPeerId = destinationPeerId,
            nextHopPeerId = PeerIdHex(value = "8899aabb"),
            metric = 2,
            sequenceNumber = 2,
        )
        engine.processUpdate(update = firstUpdate)
        engine.processUpdate(update = secondUpdate)

        // Act
        val actual = engine.routesFor(destinationPeerId = destinationPeerId)

        // Assert
        assertEquals(expected = 2, actual = actual.size)
        assertEquals(expected = PeerIdHex(value = "44556677"), actual = actual.first().nextHopPeerId)
    }

    @Test
    public fun routingUpdate_rejectsInvalidMetricSequenceNumberAndExpiryValues(): Unit {
        // Arrange
        // Act
        val metricError = assertFailsWith<IllegalArgumentException> {
            routingUpdate(metric = -1)
        }
        val sequenceNumberError = assertFailsWith<IllegalArgumentException> {
            routingUpdate(sequenceNumber = -1)
        }
        val expiryError = assertFailsWith<IllegalArgumentException> {
            routingUpdate(expiresAtEpochMillis = -1L)
        }

        // Assert
        assertEquals(expected = "RoutingUpdate metric must be greater than or equal to 0.", actual = metricError.message)
        assertEquals(expected = "RoutingUpdate sequenceNumber must be greater than or equal to 0.", actual = sequenceNumberError.message)
        assertEquals(expected = "RoutingUpdate expiresAtEpochMillis must be greater than or equal to 0.", actual = expiryError.message)
    }

    private fun routingUpdate(
        destinationPeerId: PeerIdHex = PeerIdHex(value = "00112233"),
        nextHopPeerId: PeerIdHex = PeerIdHex(value = "44556677"),
        metric: Int = 1,
        sequenceNumber: Int = 0,
        expiresAtEpochMillis: Long = 100L,
    ): RoutingUpdate {
        return RoutingUpdate(
            destinationPeerId = destinationPeerId,
            nextHopPeerId = nextHopPeerId,
            metric = metric,
            sequenceNumber = sequenceNumber,
            expiresAtEpochMillis = expiresAtEpochMillis,
        )
    }
}
