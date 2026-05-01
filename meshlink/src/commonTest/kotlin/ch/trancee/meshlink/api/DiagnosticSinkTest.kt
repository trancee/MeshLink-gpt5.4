package ch.trancee.meshlink.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

public class DiagnosticSinkTest {
  @Test
  public fun create_rejectsNonPositiveBufferSizes(): Unit {
    // Arrange
    val expectedMessage = "DiagnosticSink bufferSize must be greater than 0."

    // Act
    val error = assertFailsWith<IllegalArgumentException> { DiagnosticSink.create(bufferSize = 0) }

    // Assert
    assertEquals(expected = expectedMessage, actual = error.message)
  }

  @Test
  public fun create_usesDefaultArgumentsIncludingTheSystemClock(): Unit {
    // Arrange
    val sink = DiagnosticSink.create()

    // Act
    sink.emit(code = DiagnosticCode.ENGINE_STARTED)
    val actualTimestamp: Long = sink.diagnosticEvents.replayCache.single().timestampEpochMillis

    // Assert
    assertTrue(
      actual = actualTimestamp > 0L,
      message = "DiagnosticSink.create should default timestamps from the system clock",
    )
  }

  @Test
  public fun emit_publishesEventWithSeverityTimestampAndPayload(): Unit {
    // Arrange
    val sink = DiagnosticSink.create(bufferSize = 2, clock = { 1234L })
    val expectedPayload: DiagnosticPayload = DiagnosticPayload.InternalError(message = "boom")

    // Act
    sink.emit(code = DiagnosticCode.INTERNAL_ERROR) { expectedPayload }
    val actual: DiagnosticEvent = sink.diagnosticEvents.replayCache.single()

    // Assert
    assertEquals(expected = DiagnosticCode.INTERNAL_ERROR, actual = actual.code)
    assertEquals(expected = DiagnosticSeverity.ERROR, actual = actual.severity)
    assertEquals(expected = 1234L, actual = actual.timestampEpochMillis)
    assertEquals(expected = expectedPayload, actual = actual.payload)
    assertEquals(expected = 0L, actual = sink.droppedEventCount)
  }

  @Test
  public fun emit_dropsOldestEventAndIncrementsDropCounterWhenBufferIsFull(): Unit {
    // Arrange
    val sink = DiagnosticSink.create(bufferSize = 2, clock = { 1L })

    // Act
    sink.emit(code = DiagnosticCode.ENGINE_STARTED)
    sink.emit(code = DiagnosticCode.ENGINE_PAUSED)
    sink.emit(code = DiagnosticCode.ENGINE_STOPPED)
    val actualCodes: List<DiagnosticCode> =
      sink.diagnosticEvents.replayCache.map { event -> event.code }

    // Assert
    assertEquals(
      expected = listOf(DiagnosticCode.ENGINE_PAUSED, DiagnosticCode.ENGINE_STOPPED),
      actual = actualCodes,
      message = "DiagnosticSink should retain only the newest events in its SharedFlow ring buffer",
    )
    assertEquals(
      expected = 1L,
      actual = sink.droppedEventCount,
      message = "DiagnosticSink should increment droppedEventCount when the ring buffer overflows",
    )
  }

  @Test
  public fun emit_redactsHandshakeFailurePeerIdsWhenEnabled(): Unit {
    // Arrange
    val sink = DiagnosticSink.create(bufferSize = 1, redactPeerIds = true, clock = { 9L })

    // Act
    sink.emit(code = DiagnosticCode.HANDSHAKE_FAILED) {
      DiagnosticPayload.HandshakeFailure(
        peerId = PeerIdHex(value = "0011223344556677"),
        reason = "trust mismatch",
      )
    }
    val actual = sink.diagnosticEvents.replayCache.single().payload

    // Assert
    val payload: DiagnosticPayload.HandshakeFailure =
      assertIs<DiagnosticPayload.HandshakeFailure>(actual)
    assertEquals(expected = "00112233", actual = payload.peerId.value)
    assertEquals(expected = "trust mismatch", actual = payload.reason)
  }

  @Test
  public fun emit_redactsPeerLifecyclePeerIdsWhenEnabled(): Unit {
    // Arrange
    val sink = DiagnosticSink.create(bufferSize = 1, redactPeerIds = true, clock = { 9L })

    // Act
    sink.emit(code = DiagnosticCode.PEER_DISCOVERED) {
      DiagnosticPayload.PeerLifecycle(
        peerId = PeerIdHex(value = "8899aabbccddeeff"),
        state = PeerState.Connected,
      )
    }
    val actual = sink.diagnosticEvents.replayCache.single().payload

    // Assert
    val payload: DiagnosticPayload.PeerLifecycle = assertIs<DiagnosticPayload.PeerLifecycle>(actual)
    assertEquals(expected = "8899aabb", actual = payload.peerId.value)
    assertEquals(expected = PeerState.Connected, actual = payload.state)
  }

  @Test
  public fun emit_redactsRoutingPeerIdsWhenEnabled(): Unit {
    // Arrange
    val sink = DiagnosticSink.create(bufferSize = 1, redactPeerIds = true, clock = { 9L })

    // Act
    sink.emit(code = DiagnosticCode.ROUTE_ADDED) {
      DiagnosticPayload.RoutingChange(
        destinationPeerId = PeerIdHex(value = "aabbccddeeff0011"),
        metric = 7,
      )
    }
    val actual = sink.diagnosticEvents.replayCache.single().payload

    // Assert
    val payload: DiagnosticPayload.RoutingChange = assertIs<DiagnosticPayload.RoutingChange>(actual)
    assertEquals(expected = "aabbccdd", actual = payload.destinationPeerId.value)
    assertEquals(expected = 7, actual = payload.metric)
  }

  @Test
  public fun emit_keepsNonPeerPayloadsUnchangedWhenRedactionIsEnabled(): Unit {
    // Arrange
    val sink = DiagnosticSink.create(bufferSize = 1, redactPeerIds = true, clock = { 9L })
    val expectedPayload: DiagnosticPayload =
      DiagnosticPayload.BufferPressure(usedBytes = 512, droppedEvents = 2)

    // Act
    sink.emit(code = DiagnosticCode.BUFFER_PRESSURE) { expectedPayload }
    val actual = sink.diagnosticEvents.replayCache.single().payload

    // Assert
    assertEquals(expected = expectedPayload, actual = actual)
  }

  @Test
  public fun noOpSink_doesNotEvaluatePayloadFactoryOrRetainEvents(): Unit {
    // Arrange
    var payloadEvaluated = false

    // Act
    NoOpDiagnosticSink.emit(code = DiagnosticCode.INTERNAL_ERROR) {
      payloadEvaluated = true
      DiagnosticPayload.InternalError(message = "boom")
    }

    // Assert
    assertEquals(expected = 0L, actual = NoOpDiagnosticSink.droppedEventCount)
    assertTrue(
      actual = NoOpDiagnosticSink.diagnosticEvents.replayCache.isEmpty(),
      message = "NoOpDiagnosticSink should not retain events",
    )
    assertEquals(
      expected = false,
      actual = payloadEvaluated,
      message = "NoOpDiagnosticSink should avoid evaluating payload factories",
    )
  }
}
