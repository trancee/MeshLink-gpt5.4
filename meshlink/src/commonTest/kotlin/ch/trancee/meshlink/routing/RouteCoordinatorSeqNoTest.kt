package ch.trancee.meshlink.routing

import ch.trancee.meshlink.api.PeerIdHex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class RouteCoordinatorSeqNoTest {
  @Test
  public fun isFeasible_acceptsRoutesWhenNoSourceRecordExists(): Unit {
    // Arrange
    val coordinator = RouteCoordinator()

    // Act
    val actual =
      coordinator.isFeasible(
        destinationPeerId = PeerIdHex(value = "00112233"),
        sequenceNumber = 1,
        metric = 5,
      )

    // Assert
    assertEquals(expected = true, actual = actual)
  }

  @Test
  public fun isFeasible_acceptsNewerSequenceNumbersAndBetterMetrics(): Unit {
    // Arrange
    val destinationPeerId = PeerIdHex(value = "00112233")
    val coordinator = RouteCoordinator()
    coordinator.recordAcceptedRoute(
      destinationPeerId = destinationPeerId,
      sequenceNumber = 5,
      metric = 10,
    )

    // Act
    val newerSequenceNumber =
      coordinator.isFeasible(destinationPeerId = destinationPeerId, sequenceNumber = 6, metric = 50)
    val betterMetric =
      coordinator.isFeasible(destinationPeerId = destinationPeerId, sequenceNumber = 5, metric = 9)

    // Assert
    assertEquals(expected = true, actual = newerSequenceNumber)
    assertEquals(expected = true, actual = betterMetric)
  }

  @Test
  public fun isFeasible_rejectsOlderSequenceNumbersAndWorseMetrics(): Unit {
    // Arrange
    val destinationPeerId = PeerIdHex(value = "00112233")
    val coordinator = RouteCoordinator()
    coordinator.recordAcceptedRoute(
      destinationPeerId = destinationPeerId,
      sequenceNumber = 5,
      metric = 10,
    )

    // Act
    val olderSequenceNumber =
      coordinator.isFeasible(destinationPeerId = destinationPeerId, sequenceNumber = 4, metric = 1)
    val worseMetric =
      coordinator.isFeasible(destinationPeerId = destinationPeerId, sequenceNumber = 5, metric = 10)

    // Assert
    assertEquals(expected = false, actual = olderSequenceNumber)
    assertEquals(expected = false, actual = worseMetric)
  }

  @Test
  public fun requestSequenceNumber_advancesThePendingRequestForStarvationRecovery(): Unit {
    // Arrange
    val destinationPeerId = PeerIdHex(value = "00112233")
    val coordinator = RouteCoordinator()
    coordinator.recordAcceptedRoute(
      destinationPeerId = destinationPeerId,
      sequenceNumber = 7,
      metric = 3,
    )

    // Act
    val actual = coordinator.requestSequenceNumber(destinationPeerId = destinationPeerId)

    // Assert
    assertEquals(expected = 8, actual = actual)
    assertEquals(
      expected = 8,
      actual = coordinator.pendingSequenceNumber(destinationPeerId = destinationPeerId),
    )
  }

  @Test
  public fun recordAcceptedRoute_updatesTheSourceRecordAndClearsPendingRequests(): Unit {
    // Arrange
    val destinationPeerId = PeerIdHex(value = "00112233")
    val coordinator = RouteCoordinator()
    coordinator.requestSequenceNumber(destinationPeerId = destinationPeerId)

    // Act
    coordinator.recordAcceptedRoute(
      destinationPeerId = destinationPeerId,
      sequenceNumber = 1,
      metric = 2,
    )
    val actual = coordinator.sourceRecord(destinationPeerId = destinationPeerId)

    // Assert
    assertEquals(
      expected =
        SourceRecord(destinationPeerId = destinationPeerId, sequenceNumber = 1, metric = 2),
      actual = actual,
    )
    assertEquals(
      expected = null,
      actual = coordinator.pendingSequenceNumber(destinationPeerId = destinationPeerId),
    )
  }

  @Test
  public fun withdraw_removesKnownSourceRecords(): Unit {
    // Arrange
    val destinationPeerId = PeerIdHex(value = "00112233")
    val coordinator = RouteCoordinator()
    coordinator.recordAcceptedRoute(
      destinationPeerId = destinationPeerId,
      sequenceNumber = 2,
      metric = 3,
    )

    // Act
    coordinator.withdraw(destinationPeerId = destinationPeerId)

    // Assert
    assertEquals(
      expected = null,
      actual = coordinator.sourceRecord(destinationPeerId = destinationPeerId),
    )
    assertEquals(
      expected = null,
      actual = coordinator.pendingSequenceNumber(destinationPeerId = destinationPeerId),
    )
  }

  @Test
  public fun withdraw_ignoresDestinationsWithoutSourceRecords(): Unit {
    // Arrange
    val coordinator = RouteCoordinator()
    val destinationPeerId = PeerIdHex(value = "00112233")

    // Act
    coordinator.withdraw(destinationPeerId = destinationPeerId)

    // Assert
    assertEquals(
      expected = null,
      actual = coordinator.sourceRecord(destinationPeerId = destinationPeerId),
    )
  }

  @Test
  public fun invalidInputs_areRejected(): Unit {
    // Arrange
    val coordinator = RouteCoordinator()
    val destinationPeerId = PeerIdHex(value = "00112233")

    // Act
    val sequenceNumberError =
      assertFailsWith<IllegalArgumentException> {
        coordinator.isFeasible(
          destinationPeerId = destinationPeerId,
          sequenceNumber = -1,
          metric = 0,
        )
      }
    val metricError =
      assertFailsWith<IllegalArgumentException> {
        coordinator.isFeasible(
          destinationPeerId = destinationPeerId,
          sequenceNumber = 0,
          metric = -1,
        )
      }
    val infeasibleRouteError =
      assertFailsWith<IllegalArgumentException> {
        coordinator.recordAcceptedRoute(
          destinationPeerId = destinationPeerId,
          sequenceNumber = 0,
          metric = 1,
        )
        coordinator.recordAcceptedRoute(
          destinationPeerId = destinationPeerId,
          sequenceNumber = 0,
          metric = 2,
        )
      }
    val sourceRecordSequenceError =
      assertFailsWith<IllegalArgumentException> {
        SourceRecord(destinationPeerId = destinationPeerId, sequenceNumber = -1, metric = 0)
      }
    val sourceRecordMetricError =
      assertFailsWith<IllegalArgumentException> {
        SourceRecord(destinationPeerId = destinationPeerId, sequenceNumber = 0, metric = -1)
      }

    // Assert
    assertEquals(
      expected = "RouteCoordinator sequenceNumber must be greater than or equal to 0.",
      actual = sequenceNumberError.message,
    )
    assertEquals(
      expected = "RouteCoordinator metric must be greater than or equal to 0.",
      actual = metricError.message,
    )
    assertEquals(
      expected = "RouteCoordinator rejected infeasible route for 00112233.",
      actual = infeasibleRouteError.message,
    )
    assertEquals(
      expected = "SourceRecord sequenceNumber must be greater than or equal to 0.",
      actual = sourceRecordSequenceError.message,
    )
    assertEquals(
      expected = "SourceRecord metric must be greater than or equal to 0.",
      actual = sourceRecordMetricError.message,
    )
  }
}
