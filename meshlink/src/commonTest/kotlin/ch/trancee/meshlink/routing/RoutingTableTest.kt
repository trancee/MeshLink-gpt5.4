package ch.trancee.meshlink.routing

import ch.trancee.meshlink.api.PeerIdHex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class RoutingTableTest {
  @Test
  public fun bestRoute_prefersTheLowestMetricRoute(): Unit {
    // Arrange
    val destinationPeerId = PeerIdHex(value = "00112233")
    val lowerMetricRoute =
      routeEntry(
        destinationPeerId = destinationPeerId,
        nextHopPeerId = PeerIdHex(value = "44556677"),
        metric = 1,
      )
    val higherMetricRoute =
      routeEntry(
        destinationPeerId = destinationPeerId,
        nextHopPeerId = PeerIdHex(value = "8899aabb"),
        metric = 3,
      )
    val routingTable = RoutingTable()
    routingTable.install(route = higherMetricRoute)
    routingTable.install(route = lowerMetricRoute)

    // Act
    val actual: RouteEntry? = routingTable.bestRoute(destinationPeerId = destinationPeerId)

    // Assert
    assertEquals(expected = lowerMetricRoute, actual = actual)
  }

  @Test
  public fun routesFor_sortsTiesBySequenceNumberAndThenNextHopId(): Unit {
    // Arrange
    val destinationPeerId = PeerIdHex(value = "00112233")
    val olderRoute =
      routeEntry(
        destinationPeerId = destinationPeerId,
        nextHopPeerId = PeerIdHex(value = "ccddeeff"),
        metric = 2,
        sequenceNumber = 4,
      )
    val newerRoute =
      routeEntry(
        destinationPeerId = destinationPeerId,
        nextHopPeerId = PeerIdHex(value = "44556677"),
        metric = 2,
        sequenceNumber = 7,
      )
    val tieBreakRoute =
      routeEntry(
        destinationPeerId = destinationPeerId,
        nextHopPeerId = PeerIdHex(value = "8899aabb"),
        metric = 2,
        sequenceNumber = 7,
      )
    val routingTable = RoutingTable()
    routingTable.install(route = olderRoute)
    routingTable.install(route = tieBreakRoute)
    routingTable.install(route = newerRoute)

    // Act
    val actual: List<RouteEntry> = routingTable.routesFor(destinationPeerId = destinationPeerId)

    // Assert
    assertEquals(expected = listOf(newerRoute, tieBreakRoute, olderRoute), actual = actual)
  }

  @Test
  public fun install_replacesExistingRoutesForTheSameNextHop(): Unit {
    // Arrange
    val destinationPeerId = PeerIdHex(value = "00112233")
    val nextHopPeerId = PeerIdHex(value = "44556677")
    val originalRoute =
      routeEntry(
        destinationPeerId = destinationPeerId,
        nextHopPeerId = nextHopPeerId,
        metric = 5,
        sequenceNumber = 1,
      )
    val updatedRoute =
      routeEntry(
        destinationPeerId = destinationPeerId,
        nextHopPeerId = nextHopPeerId,
        metric = 2,
        sequenceNumber = 2,
      )
    val routingTable = RoutingTable()
    routingTable.install(route = originalRoute)

    // Act
    routingTable.install(route = updatedRoute)
    val actual: List<RouteEntry> = routingTable.routesFor(destinationPeerId = destinationPeerId)

    // Assert
    assertEquals(expected = listOf(updatedRoute), actual = actual)
  }

  @Test
  public fun withdraw_removesOnlyTheSpecifiedNextHopAndPreservesAlternatives(): Unit {
    // Arrange
    val destinationPeerId = PeerIdHex(value = "00112233")
    val withdrawnRoute =
      routeEntry(
        destinationPeerId = destinationPeerId,
        nextHopPeerId = PeerIdHex(value = "44556677"),
        metric = 1,
      )
    val fallbackRoute =
      routeEntry(
        destinationPeerId = destinationPeerId,
        nextHopPeerId = PeerIdHex(value = "8899aabb"),
        metric = 2,
      )
    val routingTable = RoutingTable()
    routingTable.install(route = withdrawnRoute)
    routingTable.install(route = fallbackRoute)

    // Act
    routingTable.withdraw(
      destinationPeerId = destinationPeerId,
      nextHopPeerId = withdrawnRoute.nextHopPeerId,
    )
    val actualBestRoute: RouteEntry? = routingTable.bestRoute(destinationPeerId = destinationPeerId)
    val actualRoutes: List<RouteEntry> =
      routingTable.routesFor(destinationPeerId = destinationPeerId)

    // Assert
    assertEquals(expected = fallbackRoute, actual = actualBestRoute)
    assertEquals(expected = listOf(fallbackRoute), actual = actualRoutes)
  }

  @Test
  public fun withdraw_removesDestinationsThatNoLongerHaveRoutes(): Unit {
    // Arrange
    val route =
      routeEntry(
        destinationPeerId = PeerIdHex(value = "00112233"),
        nextHopPeerId = PeerIdHex(value = "44556677"),
      )
    val routingTable = RoutingTable()
    routingTable.install(route = route)

    // Act
    routingTable.withdraw(
      destinationPeerId = route.destinationPeerId,
      nextHopPeerId = route.nextHopPeerId,
    )

    // Assert
    assertEquals(
      expected = emptyList(),
      actual = routingTable.routesFor(destinationPeerId = route.destinationPeerId),
    )
    assertEquals(expected = emptySet(), actual = routingTable.destinations())
  }

  @Test
  public fun bestRouteAndRoutesFor_returnNullOrEmptyWhenNoDestinationExists(): Unit {
    // Arrange
    val destinationPeerId = PeerIdHex(value = "00112233")
    val routingTable = RoutingTable()

    // Act
    val actualBestRoute: RouteEntry? = routingTable.bestRoute(destinationPeerId = destinationPeerId)
    val actualRoutes: List<RouteEntry> =
      routingTable.routesFor(destinationPeerId = destinationPeerId)

    // Assert
    assertEquals(expected = null, actual = actualBestRoute)
    assertEquals(expected = emptyList(), actual = actualRoutes)
  }

  @Test
  public fun withdraw_ignoresUnknownDestinations(): Unit {
    // Arrange
    val routingTable = RoutingTable()

    // Act
    routingTable.withdraw(
      destinationPeerId = PeerIdHex(value = "00112233"),
      nextHopPeerId = PeerIdHex(value = "44556677"),
    )

    // Assert
    assertEquals(expected = emptySet(), actual = routingTable.destinations())
  }

  @Test
  public fun withdraw_ignoresMissingRoutes(): Unit {
    // Arrange
    val destinationPeerId = PeerIdHex(value = "00112233")
    val route =
      routeEntry(
        destinationPeerId = destinationPeerId,
        nextHopPeerId = PeerIdHex(value = "44556677"),
      )
    val routingTable = RoutingTable()
    routingTable.install(route = route)

    // Act
    routingTable.withdraw(
      destinationPeerId = destinationPeerId,
      nextHopPeerId = PeerIdHex(value = "8899aabb"),
    )

    // Assert
    assertEquals(
      expected = listOf(route),
      actual = routingTable.routesFor(destinationPeerId = destinationPeerId),
    )
  }

  @Test
  public fun destinations_returnsAllKnownDestinations(): Unit {
    // Arrange
    val firstDestination = PeerIdHex(value = "00112233")
    val secondDestination = PeerIdHex(value = "44556677")
    val routingTable = RoutingTable()
    routingTable.install(
      route =
        routeEntry(
          destinationPeerId = firstDestination,
          nextHopPeerId = PeerIdHex(value = "8899aabb"),
        )
    )
    routingTable.install(
      route =
        routeEntry(
          destinationPeerId = secondDestination,
          nextHopPeerId = PeerIdHex(value = "ccddeeff"),
        )
    )

    // Act
    val actual: Set<PeerIdHex> = routingTable.destinations()

    // Assert
    assertEquals(expected = linkedSetOf(firstDestination, secondDestination), actual = actual)
  }

  @Test
  public fun routeEntry_rejectsInvalidMetricsSequenceNumbersAndExpiryValues(): Unit {
    // Arrange
    // Act
    val metricError = assertFailsWith<IllegalArgumentException> { routeEntry(metric = -1) }
    val sequenceNumberError =
      assertFailsWith<IllegalArgumentException> { routeEntry(sequenceNumber = -1) }
    val expiryError =
      assertFailsWith<IllegalArgumentException> { routeEntry(expiresAtEpochMillis = -1L) }

    // Assert
    assertEquals(
      expected = "RouteEntry metric must be greater than or equal to 0.",
      actual = metricError.message,
    )
    assertEquals(
      expected = "RouteEntry sequenceNumber must be greater than or equal to 0.",
      actual = sequenceNumberError.message,
    )
    assertEquals(
      expected = "RouteEntry expiresAtEpochMillis must be greater than or equal to 0.",
      actual = expiryError.message,
    )
  }

  private fun routeEntry(
    destinationPeerId: PeerIdHex = PeerIdHex(value = "00112233"),
    nextHopPeerId: PeerIdHex = PeerIdHex(value = "44556677"),
    metric: Int = 1,
    sequenceNumber: Int = 0,
    expiresAtEpochMillis: Long = 1L,
  ): RouteEntry {
    return RouteEntry(
      destinationPeerId = destinationPeerId,
      nextHopPeerId = nextHopPeerId,
      metric = metric,
      sequenceNumber = sequenceNumber,
      expiresAtEpochMillis = expiresAtEpochMillis,
    )
  }
}
