package ch.trancee.meshlink.messaging

import ch.trancee.meshlink.api.DiagnosticCode
import ch.trancee.meshlink.api.DiagnosticPayload
import ch.trancee.meshlink.api.DiagnosticSink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

public class CutThroughBufferDiagnosticTest {
  @Test
  public fun relayChunk0_emitsMessageSentDiagnosticsForCutThroughRelay(): Unit {
    // Arrange
    val diagnosticSink = DiagnosticSink.create(bufferSize = 4, clock = { 30L })
    val pipeline =
      DeliveryPipeline(config = MessagingConfig.default(), diagnosticSink = diagnosticSink)

    // Act
    pipeline.relayChunk0(chunk0 = byteArrayOf(0x01, 0x02), localHopPeerId = byteArrayOf(0x0A))
    val actualEvent = diagnosticSink.diagnosticEvents.replayCache.single()

    // Assert
    assertEquals(expected = DiagnosticCode.MESSAGE_SENT, actual = actualEvent.code)
    val payload = assertIs<DiagnosticPayload.InternalError>(actualEvent.payload)
    assertEquals(expected = "cut-through-relay", actual = payload.message)
  }
}
