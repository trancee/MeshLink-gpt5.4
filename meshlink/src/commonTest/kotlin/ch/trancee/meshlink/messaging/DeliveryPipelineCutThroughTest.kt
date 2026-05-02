package ch.trancee.meshlink.messaging

import kotlin.test.Test
import kotlin.test.assertContentEquals

public class DeliveryPipelineCutThroughTest {
  @Test
  public fun relayChunk0_forwardsTheFirstChunkImmediatelyWithTheVisitedHopAppended(): Unit {
    // Arrange
    val pipeline = DeliveryPipeline(config = MessagingConfig.default())
    val chunk0 = byteArrayOf(0x01, 0x02, 0x03)
    val localHopPeerId = byteArrayOf(0x0A, 0x0B)

    // Act
    val actual = pipeline.relayChunk0(chunk0 = chunk0, localHopPeerId = localHopPeerId)

    // Assert
    assertContentEquals(expected = byteArrayOf(0x01, 0x02, 0x03, 0x02, 0x0A, 0x0B), actual = actual)
  }
}
