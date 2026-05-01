package ch.trancee.meshlink.routing

import ch.trancee.meshlink.api.PeerIdHex
import kotlin.test.Test
import kotlin.test.assertEquals

public class RoutingTest {
    @Test
    public fun engines_convergeToTheLowestMetricMultiHopRouteAcrossTopologyChanges(): Unit {
        // Arrange
        val destinationPeerId = PeerIdHex(value = "00112233")
        val firstRelayPeerId = PeerIdHex(value = "44556677")
        val secondRelayPeerId = PeerIdHex(value = "8899aabb")
        val firstEngine = RoutingEngine(config = RoutingConfig.default())
        val secondEngine = RoutingEngine(config = RoutingConfig.default())

        // Act
        firstEngine.processUpdate(
            update = RoutingUpdate(
                destinationPeerId = destinationPeerId,
                nextHopPeerId = firstRelayPeerId,
                metric = 3,
                sequenceNumber = 1,
                expiresAtEpochMillis = 100L,
            ),
        )
        firstEngine.processUpdate(
            update = RoutingUpdate(
                destinationPeerId = destinationPeerId,
                nextHopPeerId = secondRelayPeerId,
                metric = 2,
                sequenceNumber = 2,
                expiresAtEpochMillis = 100L,
            ),
        )
        secondEngine.processUpdate(
            update = RoutingUpdate(
                destinationPeerId = destinationPeerId,
                nextHopPeerId = secondRelayPeerId,
                metric = 2,
                sequenceNumber = 2,
                expiresAtEpochMillis = 100L,
            ),
        )
        secondEngine.processUpdate(
            update = RoutingUpdate(
                destinationPeerId = destinationPeerId,
                nextHopPeerId = secondRelayPeerId,
                metric = RoutingEngine.INFINITE_METRIC,
                sequenceNumber = 3,
                expiresAtEpochMillis = 100L,
            ),
        )
        secondEngine.processUpdate(
            update = RoutingUpdate(
                destinationPeerId = destinationPeerId,
                nextHopPeerId = firstRelayPeerId,
                metric = 1,
                sequenceNumber = 4,
                expiresAtEpochMillis = 100L,
            ),
        )

        // Assert
        assertEquals(expected = secondRelayPeerId, actual = firstEngine.nextHopFor(destinationPeerId = destinationPeerId))
        assertEquals(expected = firstRelayPeerId, actual = secondEngine.nextHopFor(destinationPeerId = destinationPeerId))
    }
}
