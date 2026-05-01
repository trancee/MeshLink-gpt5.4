package ch.trancee.meshlink.routing

import ch.trancee.meshlink.api.DiagnosticCode
import ch.trancee.meshlink.api.DiagnosticPayload
import ch.trancee.meshlink.api.DiagnosticSink
import ch.trancee.meshlink.api.PeerIdHex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

public class RouteCoordinatorDiagnosticTest {
  @Test
  public fun recordAcceptedRoute_emitsRouteAddedDiagnostics(): Unit {
    // Arrange
    val diagnosticSink = DiagnosticSink.create(bufferSize = 4, clock = { 10L })
    val destinationPeerId = PeerIdHex(value = "00112233")
    val coordinator = RouteCoordinator(diagnosticSink = diagnosticSink)

    // Act
    coordinator.recordAcceptedRoute(
      destinationPeerId = destinationPeerId,
      sequenceNumber = 1,
      metric = 3,
    )
    val actualEvent = diagnosticSink.diagnosticEvents.replayCache.single()

    // Assert
    assertEquals(expected = DiagnosticCode.ROUTE_ADDED, actual = actualEvent.code)
    val payload = assertIs<DiagnosticPayload.RoutingChange>(actualEvent.payload)
    assertEquals(expected = destinationPeerId, actual = payload.destinationPeerId)
    assertEquals(expected = 3, actual = payload.metric)
  }

  @Test
  public fun requestSequenceNumber_andWithdraw_emitRouteRemovedDiagnostics(): Unit {
    // Arrange
    val diagnosticSink = DiagnosticSink.create(bufferSize = 8, clock = { 20L })
    val destinationPeerId = PeerIdHex(value = "00112233")
    val coordinator = RouteCoordinator(diagnosticSink = diagnosticSink)
    coordinator.recordAcceptedRoute(
      destinationPeerId = destinationPeerId,
      sequenceNumber = 4,
      metric = 2,
    )

    // Act
    val requestedSequenceNumber =
      coordinator.requestSequenceNumber(destinationPeerId = destinationPeerId)
    coordinator.withdraw(destinationPeerId = destinationPeerId)
    val actualCodes = diagnosticSink.diagnosticEvents.replayCache.map { event -> event.code }
    val requestPayload =
      assertIs<DiagnosticPayload.RoutingChange>(
        diagnosticSink.diagnosticEvents.replayCache[1].payload
      )
    val withdrawPayload =
      assertIs<DiagnosticPayload.RoutingChange>(
        diagnosticSink.diagnosticEvents.replayCache[2].payload
      )

    // Assert
    assertEquals(expected = 5, actual = requestedSequenceNumber)
    assertEquals(
      expected =
        listOf(
          DiagnosticCode.ROUTE_ADDED,
          DiagnosticCode.ROUTE_REMOVED,
          DiagnosticCode.ROUTE_REMOVED,
        ),
      actual = actualCodes,
    )
    assertEquals(expected = destinationPeerId, actual = requestPayload.destinationPeerId)
    assertEquals(expected = 5, actual = requestPayload.metric)
    assertEquals(expected = destinationPeerId, actual = withdrawPayload.destinationPeerId)
    assertEquals(expected = 0, actual = withdrawPayload.metric)
  }
}
