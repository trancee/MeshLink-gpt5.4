package ch.trancee.meshlink.messaging

import kotlin.test.Test
import kotlin.test.assertContentEquals

public class CutThroughRelayIntegrationTest {
  @Test
  public fun relayPipeline_appendsVisitedHopsAcrossThreeNodes(): Unit {
    // Arrange
    val firstRelay = DeliveryPipeline(config = MessagingConfig.default())
    val secondRelay = DeliveryPipeline(config = MessagingConfig.default())
    val chunk0 = byteArrayOf(0x01, 0x02, 0x03)
    val firstHopPeerId = byteArrayOf(0x0A)
    val secondHopPeerId = byteArrayOf(0x0B)

    // Act
    val firstForwardedFrame =
      firstRelay.relayChunk0(chunk0 = chunk0, localHopPeerId = firstHopPeerId)
    val secondForwardedFrame =
      secondRelay.relayChunk0(chunk0 = firstForwardedFrame, localHopPeerId = secondHopPeerId)

    // Assert
    assertContentEquals(
      expected = byteArrayOf(0x01, 0x02, 0x03, 0x0A, 0x0B),
      actual = secondForwardedFrame,
    )
  }
}
