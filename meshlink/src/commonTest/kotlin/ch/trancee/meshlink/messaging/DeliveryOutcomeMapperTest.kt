package ch.trancee.meshlink.messaging

import ch.trancee.meshlink.api.PeerIdHex
import kotlin.test.Test
import kotlin.test.assertEquals

public class DeliveryOutcomeMapperTest {
  @Test
  public fun failed_mapsMessageIdentifiersPeersAndReasonsIntoDeliveryFailedOutcomes(): Unit {
    // Arrange
    val messageId = MessageIdKey(senderPeerId = PeerIdHex(value = "00112233"), sequenceNumber = 7L)
    val recipientPeerId = PeerIdHex(value = "44556677")

    // Act
    val actual =
      DeliveryOutcomeMapper.failed(
        messageId = messageId,
        peerId = recipientPeerId,
        reason = DeliveryFailureReason.UNREACHABLE,
      )

    // Assert
    assertEquals(
      expected =
        DeliveryFailed(
          messageId = messageId,
          peerId = recipientPeerId,
          reason = DeliveryFailureReason.UNREACHABLE,
        ),
      actual = actual,
    )
  }
}
