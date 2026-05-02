package ch.trancee.meshlink.routing

import ch.trancee.meshlink.harness.MeshTestHarness
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

public class RoutingConvergenceIntegrationTest {
  @Test
  public fun linearTenNodeTopology_convergesWithinThreeSeconds(): Unit {
    // Arrange
    val harness = MeshTestHarness.createLinearNetwork(size = 10)
    val routingConfig =
      RoutingConfig(routeExpiryMillis = 30_000L, peerTimeoutMillis = 15_000L, hopLimit = 10)
    val engines = List(size = harness.peerIds.size) { RoutingEngine(config = routingConfig) }
    val destinationPeerId = harness.peerIds.last()

    // Act
    val elapsedMillis = measureTimeMillis {
      for (index in harness.peerIds.lastIndex - 1 downTo 0) {
        val distanceToDestination: Int =
          destinationPeerIdDistance(index = index, lastIndex = harness.peerIds.lastIndex)
        val nextHopPeerId = harness.peerIds[index + 1]
        val accepted =
          engines[index].processUpdate(
            update =
              RoutingUpdate(
                destinationPeerId = destinationPeerId,
                nextHopPeerId = nextHopPeerId,
                metric = distanceToDestination,
                sequenceNumber = 1,
                expiresAtEpochMillis = 100L,
              )
          )
        assertTrue(
          actual = accepted,
          message =
            "RoutingConvergenceIntegrationTest should accept each linear-topology update during convergence.",
        )
      }
    }
    val actual = engines.first().nextHopFor(destinationPeerId = destinationPeerId)

    // Assert
    assertEquals(
      expected = harness.peerIds[1],
      actual = actual,
      message =
        "RoutingConvergenceIntegrationTest should converge on the first neighbor as the next hop to the farthest node.",
    )
    assertTrue(
      actual = elapsedMillis < 3_000L,
      message =
        "RoutingConvergenceIntegrationTest should complete the 10-node topology change within 3 seconds.",
    )
  }

  private fun destinationPeerIdDistance(index: Int, lastIndex: Int): Int {
    return lastIndex - index
  }
}
