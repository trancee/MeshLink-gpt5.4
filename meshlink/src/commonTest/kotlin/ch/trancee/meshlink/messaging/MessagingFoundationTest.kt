package ch.trancee.meshlink.messaging

import ch.trancee.meshlink.api.PeerIdHex
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class MessagingFoundationTest {
  @Test
  public fun messagingConfig_defaultExposesExpectedRateLimitAndTimeoutValues(): Unit {
    // Arrange
    // Act
    val actual = MessagingConfig.default()

    // Assert
    assertEquals(expected = 1_000L, actual = actual.rateLimitWindowMillis)
    assertEquals(expected = 32, actual = actual.maxMessagesPerWindow)
    assertEquals(expected = 5_000L, actual = actual.deliveryTimeoutMillis)
    assertEquals(expected = 64, actual = actual.maxPendingMessages)
    assertEquals(expected = 0, actual = actual.appIdHash)
  }

  @Test
  public fun messagingConfig_rejectsNonPositiveWindowLimitTimeoutAndBufferValues(): Unit {
    // Arrange
    // Act
    val windowError = assertFailsWith<IllegalArgumentException> { MessagingConfig(0L, 1, 1L, 1, 0) }
    val maxMessagesError =
      assertFailsWith<IllegalArgumentException> { MessagingConfig(1L, 0, 1L, 1, 0) }
    val timeoutError =
      assertFailsWith<IllegalArgumentException> { MessagingConfig(1L, 1, 0L, 1, 0) }
    val pendingError =
      assertFailsWith<IllegalArgumentException> { MessagingConfig(1L, 1, 1L, 0, 0) }

    // Assert
    assertEquals(
      expected = "MessagingConfig rateLimitWindowMillis must be greater than 0.",
      actual = windowError.message,
    )
    assertEquals(
      expected = "MessagingConfig maxMessagesPerWindow must be greater than 0.",
      actual = maxMessagesError.message,
    )
    assertEquals(
      expected = "MessagingConfig deliveryTimeoutMillis must be greater than 0.",
      actual = timeoutError.message,
    )
    assertEquals(
      expected = "MessagingConfig maxPendingMessages must be greater than 0.",
      actual = pendingError.message,
    )
  }

  @Test
  public fun coreMessagingTypes_retainConstructorValues(): Unit {
    // Arrange
    val senderPeerId = PeerIdHex(value = "00112233")
    val recipientPeerId = PeerIdHex(value = "44556677")
    val messageId = MessageIdKey(senderPeerId = senderPeerId, sequenceNumber = 7L)
    val payload = byteArrayOf(0x01, 0x02, 0x03)

    // Act
    val peerPair = PeerPair(senderPeerId = senderPeerId, recipientPeerId = recipientPeerId)
    val inboundMessage =
      InboundMessage(messageId = messageId, fromPeerId = senderPeerId, payload = payload)
    val delivered: DeliveryOutcome = Delivered(messageId = messageId, peerId = recipientPeerId)
    val failed: DeliveryOutcome =
      DeliveryFailed(
        messageId = messageId,
        peerId = recipientPeerId,
        reason = DeliveryFailureReason.TIMEOUT,
      )
    val sent: SendResult = SendResult.Sent(messageId = messageId)
    val queued: SendResult =
      SendResult.Queued(messageId = messageId, reason = QueuedReason.RATE_LIMITED)
    val rejected: SendResult = SendResult.Rejected(reason = DeliveryFailureReason.BUFFER_PRESSURE)

    // Assert
    assertEquals(expected = senderPeerId, actual = peerPair.senderPeerId)
    assertEquals(expected = recipientPeerId, actual = peerPair.recipientPeerId)
    assertEquals(expected = messageId, actual = inboundMessage.messageId)
    assertEquals(expected = senderPeerId, actual = inboundMessage.fromPeerId)
    assertContentEquals(expected = payload, actual = inboundMessage.payload)
    assertEquals(
      expected = Delivered(messageId = messageId, peerId = recipientPeerId),
      actual = delivered,
    )
    assertEquals(
      expected =
        DeliveryFailed(
          messageId = messageId,
          peerId = recipientPeerId,
          reason = DeliveryFailureReason.TIMEOUT,
        ),
      actual = failed,
    )
    assertEquals(expected = SendResult.Sent(messageId = messageId), actual = sent)
    assertEquals(
      expected = SendResult.Queued(messageId = messageId, reason = QueuedReason.RATE_LIMITED),
      actual = queued,
    )
    assertEquals(
      expected = SendResult.Rejected(reason = DeliveryFailureReason.BUFFER_PRESSURE),
      actual = rejected,
    )
  }

  @Test
  public fun messageIdKey_rejectsNegativeSequenceNumbers(): Unit {
    // Arrange
    // Act
    val error =
      assertFailsWith<IllegalArgumentException> {
        MessageIdKey(senderPeerId = PeerIdHex(value = "00112233"), sequenceNumber = -1L)
      }

    // Assert
    assertEquals(
      expected = "MessageIdKey sequenceNumber must be greater than or equal to 0.",
      actual = error.message,
    )
  }
}
