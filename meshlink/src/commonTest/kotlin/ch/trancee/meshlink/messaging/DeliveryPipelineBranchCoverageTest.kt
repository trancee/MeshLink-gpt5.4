package ch.trancee.meshlink.messaging

import ch.trancee.meshlink.api.PeerIdHex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

public class DeliveryPipelineBranchCoverageTest {
    @Test
    public fun send_returnsQueuedResultsWhenTheRateLimiterRejectsBurstTraffic(): Unit {
        // Arrange
        val senderPeerId = PeerIdHex(value = "00112233")
        val recipientPeerId = PeerIdHex(value = "44556677")
        val pipeline = DeliveryPipeline(
            config = MessagingConfig(
                rateLimitWindowMillis = 1_000L,
                maxMessagesPerWindow = 1,
                deliveryTimeoutMillis = 5_000L,
                maxPendingMessages = 10,
                appIdHash = 0,
            ),
        )
        pipeline.send(senderPeerId = senderPeerId, recipientPeerId = recipientPeerId, payload = byteArrayOf(0x01), nowEpochMillis = 0L)

        // Act
        val actual = pipeline.send(
            senderPeerId = senderPeerId,
            recipientPeerId = recipientPeerId,
            payload = byteArrayOf(0x02),
            nowEpochMillis = 1L,
        )

        // Assert
        val queued = assertIs<SendResult.Queued>(actual)
        assertEquals(expected = QueuedReason.RATE_LIMITED, actual = queued.reason)
    }

    @Test
    public fun acknowledge_returnsNullWhenAMessageIsNotPending(): Unit {
        // Arrange
        val pipeline = DeliveryPipeline(config = MessagingConfig.default())
        val messageId = MessageIdKey(senderPeerId = PeerIdHex(value = "00112233"), sequenceNumber = 9L)

        // Act
        val actual = pipeline.acknowledge(messageId = messageId)

        // Assert
        assertEquals(expected = null, actual = actual)
    }

    @Test
    public fun failTimedOut_returnsAnEmptyListWhenNothingHasExpired(): Unit {
        // Arrange
        val senderPeerId = PeerIdHex(value = "00112233")
        val recipientPeerId = PeerIdHex(value = "44556677")
        val pipeline = DeliveryPipeline(config = MessagingConfig.default())
        pipeline.send(
            senderPeerId = senderPeerId,
            recipientPeerId = recipientPeerId,
            payload = byteArrayOf(0x01),
            nowEpochMillis = 0L,
        )

        // Act
        val actual = pipeline.failTimedOut(nowEpochMillis = MessagingConfig.default().deliveryTimeoutMillis - 1L)

        // Assert
        assertEquals(expected = emptyList(), actual = actual)
    }

    @Test
    public fun failTimedOut_rejectsNegativeTimestamps(): Unit {
        // Arrange
        val pipeline = DeliveryPipeline(config = MessagingConfig.default())

        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            pipeline.failTimedOut(nowEpochMillis = -1L)
        }

        // Assert
        assertEquals(expected = "DeliveryPipeline nowEpochMillis must be greater than or equal to 0.", actual = error.message)
    }

    @Test
    public fun send_rejectsNegativeTimestamps(): Unit {
        // Arrange
        val pipeline = DeliveryPipeline(config = MessagingConfig.default())

        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            pipeline.send(
                senderPeerId = PeerIdHex(value = "00112233"),
                recipientPeerId = PeerIdHex(value = "44556677"),
                payload = byteArrayOf(0x01),
                nowEpochMillis = -1L,
            )
        }

        // Assert
        assertEquals(expected = "DeliveryPipeline nowEpochMillis must be greater than or equal to 0.", actual = error.message)
    }
}
