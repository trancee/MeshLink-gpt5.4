package ch.trancee.meshlink.routing

import ch.trancee.meshlink.harness.MeshTestHarness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

public class RoutingHarnessIntegrationTest {
  @Test
  public fun linearHarness_propagatesAndWithdrawsMultiHopRoutes(): Unit {
    // Arrange
    val harness = MeshTestHarness.createLinearNetwork(size = 3)
    val engines =
      List(size = harness.peerIds.size) { RoutingEngine(config = RoutingConfig.default()) }
    val destinationPeerId = harness.peerIds.last()

    // Act
    val intermediateAccepted =
      engines[1].processUpdate(
        update =
          RoutingUpdate(
            destinationPeerId = destinationPeerId,
            nextHopPeerId = destinationPeerId,
            metric = 1,
            sequenceNumber = 1,
            expiresAtEpochMillis = 100L,
          )
      )
    val sourceAccepted =
      engines[0].processUpdate(
        update =
          RoutingUpdate(
            destinationPeerId = destinationPeerId,
            nextHopPeerId = harness.peerIds[1],
            metric = 2,
            sequenceNumber = 1,
            expiresAtEpochMillis = 100L,
          )
      )
    val nextHopBeforeWithdraw = engines[0].nextHopFor(destinationPeerId = destinationPeerId)
    engines[1].processUpdate(
      update =
        RoutingUpdate(
          destinationPeerId = destinationPeerId,
          nextHopPeerId = destinationPeerId,
          metric = RoutingEngine.INFINITE_METRIC,
          sequenceNumber = 2,
          expiresAtEpochMillis = 200L,
        )
    )
    engines[0].processUpdate(
      update =
        RoutingUpdate(
          destinationPeerId = destinationPeerId,
          nextHopPeerId = harness.peerIds[1],
          metric = RoutingEngine.INFINITE_METRIC,
          sequenceNumber = 2,
          expiresAtEpochMillis = 200L,
        )
    )
    val nextHopAfterWithdraw = engines[0].nextHopFor(destinationPeerId = destinationPeerId)

    // Assert
    assertEquals(expected = true, actual = intermediateAccepted)
    assertEquals(expected = true, actual = sourceAccepted)
    assertEquals(
      expected = harness.peerIds[1],
      actual = nextHopBeforeWithdraw,
      message =
        "RoutingHarnessIntegrationTest should select the intermediate peer as the multi-hop next hop.",
    )
    assertNull(
      actual = nextHopAfterWithdraw,
      message =
        "RoutingHarnessIntegrationTest should remove the propagated route after the withdrawal reaches the source node.",
    )
  }
}
