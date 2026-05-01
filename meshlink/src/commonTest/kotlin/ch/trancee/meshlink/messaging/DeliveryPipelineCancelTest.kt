package ch.trancee.meshlink.messaging

import ch.trancee.meshlink.api.PeerIdHex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

public class DeliveryPipelineCancelTest {
  @Test
  public fun cancel_releasesPendingMessagesAndReturnsCancelledFailures(): Unit {
    // Arrange
    val senderPeerId = PeerIdHex(value = "00112233")
    val recipientPeerId = PeerIdHex(value = "44556677")
    val pipeline = DeliveryPipeline(config = MessagingConfig.default())
    val sendResult =
      pipeline.send(
        senderPeerId = senderPeerId,
        recipientPeerId = recipientPeerId,
        payload = byteArrayOf(0x01),
        nowEpochMillis = 0L,
      )
    val messageId = assertIs<SendResult.Sent>(sendResult).messageId

    // Act
    val actual = pipeline.cancel(messageId = messageId)
    val secondCancel = pipeline.cancel(messageId = messageId)

    // Assert
    val failed = assertIs<DeliveryFailed>(actual)
    assertEquals(expected = DeliveryFailureReason.CANCELLED, actual = failed.reason)
    assertEquals(expected = 0, actual = pipeline.pendingCount())
    assertNull(secondCancel)
  }
}
