package ch.trancee.meshlink.messaging

import ch.trancee.meshlink.api.DiagnosticCode
import ch.trancee.meshlink.api.DiagnosticSink
import ch.trancee.meshlink.api.PeerIdHex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

public class DiagnosticEmissionIntegrationTest {
  @Test
  public fun pipeline_emitsDiagnosticsAcrossSendCancelTimeoutAndCutThroughScenarios(): Unit {
    // Arrange
    val senderPeerId = PeerIdHex(value = "00112233")
    val recipientPeerId = PeerIdHex(value = "44556677")
    val diagnosticSink = DiagnosticSink.create(bufferSize = 16, clock = { 50L })
    val pipeline =
      DeliveryPipeline(
        config =
          MessagingConfig(
            rateLimitWindowMillis = 1_000L,
            maxMessagesPerWindow = 10,
            deliveryTimeoutMillis = 100L,
            maxPendingMessages = 2,
            appIdHash = 0,
          ),
        diagnosticSink = diagnosticSink,
      )
    val cancelMessageId =
      assertIs<SendResult.Sent>(
          pipeline.send(
            senderPeerId = senderPeerId,
            recipientPeerId = recipientPeerId,
            payload = byteArrayOf(0x01),
            nowEpochMillis = 0L,
          )
        )
        .messageId
    assertIs<SendResult.Sent>(
      pipeline.send(
        senderPeerId = senderPeerId,
        recipientPeerId = recipientPeerId,
        payload = byteArrayOf(0x02),
        nowEpochMillis = 1L,
      )
    )

    // Act
    pipeline.cancel(messageId = cancelMessageId)
    pipeline.failTimedOut(nowEpochMillis = 101L)
    pipeline.relayChunk0(chunk0 = byteArrayOf(0x03), localHopPeerId = byteArrayOf(0x0A))
    val actualCodes = diagnosticSink.diagnosticEvents.replayCache.map { event -> event.code }

    // Assert
    assertEquals(
      expected =
        listOf(
          DiagnosticCode.MESSAGE_SENT,
          DiagnosticCode.MESSAGE_SENT,
          DiagnosticCode.MESSAGE_FAILED,
          DiagnosticCode.MESSAGE_FAILED,
          DiagnosticCode.MESSAGE_SENT,
        ),
      actual = actualCodes,
    )
  }
}
