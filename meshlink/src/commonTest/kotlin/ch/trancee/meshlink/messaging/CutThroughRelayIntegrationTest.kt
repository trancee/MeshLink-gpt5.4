package ch.trancee.meshlink.messaging

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

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
      expected = byteArrayOf(0x01, 0x02, 0x03, 0x01, 0x0A, 0x01, 0x0B),
      actual = secondForwardedFrame,
    )
    val visitedHops =
      CutThroughBuffer().visitedHops(chunk0 = secondForwardedFrame, payloadSizeBytes = 3)
    assertEquals(expected = 2, actual = visitedHops.size)
    assertContentEquals(expected = firstHopPeerId, actual = visitedHops[0])
    assertContentEquals(expected = secondHopPeerId, actual = visitedHops[1])
  }
}
