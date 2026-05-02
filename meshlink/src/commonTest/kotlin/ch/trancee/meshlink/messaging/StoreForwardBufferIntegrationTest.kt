package ch.trancee.meshlink.messaging

import ch.trancee.meshlink.api.PeerIdHex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

public class StoreForwardBufferIntegrationTest {
  @Test
  public fun bufferedMessages_flushInOriginalOrderWhenARouteBecomesAvailable(): Unit {
    // Arrange
    val senderPeerId = PeerIdHex(value = "00112233")
    val recipientPeerId = PeerIdHex(value = "44556677")
    val pipeline = MessagingConfig.default().let(::DeliveryPipeline)
    val firstPayload = byteArrayOf(0x01)
    val secondPayload = byteArrayOf(0x02)

    // Act
    val firstQueued =
      pipeline.bufferForUnavailableRoute(
        senderPeerId = senderPeerId,
        recipientPeerId = recipientPeerId,
        payload = firstPayload,
        nowEpochMillis = 0L,
      )
    val secondQueued =
      pipeline.bufferForUnavailableRoute(
        senderPeerId = senderPeerId,
        recipientPeerId = recipientPeerId,
        payload = secondPayload,
        nowEpochMillis = 1L,
      )
    val flushed = pipeline.flushBuffered(recipientPeerId = recipientPeerId, nowEpochMillis = 2L)

    // Assert
    assertEquals(expected = 2, actual = flushed.size)
    assertEquals(expected = 0, actual = pipeline.bufferedCount())
    assertEquals(expected = 2, actual = pipeline.pendingCount())
    val firstQueuedResult = assertIs<SendResult.Queued>(firstQueued)
    val secondQueuedResult = assertIs<SendResult.Queued>(secondQueued)
    assertEquals(expected = QueuedReason.ROUTE_UNAVAILABLE, actual = firstQueuedResult.reason)
    assertEquals(expected = QueuedReason.ROUTE_UNAVAILABLE, actual = secondQueuedResult.reason)
    val firstFlushed = assertIs<SendResult.Sent>(flushed[0])
    val secondFlushed = assertIs<SendResult.Sent>(flushed[1])
    assertEquals(expected = firstQueuedResult.messageId, actual = firstFlushed.messageId)
    assertEquals(expected = secondQueuedResult.messageId, actual = secondFlushed.messageId)
  }

  @Test
  public fun bufferedMessages_evictOldestEntriesWhenCapacityIsExceeded(): Unit {
    // Arrange
    val senderPeerId = PeerIdHex(value = "00112233")
    val recipientPeerId = PeerIdHex(value = "44556677")
    val pipeline =
      DeliveryPipeline(
        config =
          MessagingConfig(
            rateLimitWindowMillis = 1_000L,
            maxMessagesPerWindow = 32,
            deliveryTimeoutMillis = 5_000L,
            maxPendingMessages = 64,
            appIdHash = 0,
            maxBufferedMessages = 1,
          )
      )

    // Act
    pipeline.bufferForUnavailableRoute(
      senderPeerId = senderPeerId,
      recipientPeerId = recipientPeerId,
      payload = byteArrayOf(0x01),
      nowEpochMillis = 0L,
    )
    val queued =
      pipeline.bufferForUnavailableRoute(
        senderPeerId = senderPeerId,
        recipientPeerId = recipientPeerId,
        payload = byteArrayOf(0x02),
        nowEpochMillis = 1L,
      )
    val flushed = pipeline.flushBuffered(recipientPeerId = recipientPeerId, nowEpochMillis = 2L)

    // Assert
    assertEquals(expected = 1, actual = pipeline.pendingCount())
    val queuedResult = assertIs<SendResult.Queued>(queued)
    val flushedResult = assertIs<SendResult.Sent>(flushed.single())
    assertEquals(expected = queuedResult.messageId, actual = flushedResult.messageId)
  }
}
