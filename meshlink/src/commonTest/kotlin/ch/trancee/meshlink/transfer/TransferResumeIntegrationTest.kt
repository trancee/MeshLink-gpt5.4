package ch.trancee.meshlink.transfer

import ch.trancee.meshlink.api.PeerIdHex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

public class TransferResumeIntegrationTest {
  @Test
  public fun retransmitLimit_andResumeOffset_onlyRetryMissingChunksWithinTheAllowedBudget(): Unit {
    // Arrange
    val engine =
      TransferEngine(
        config = TransferConfig(timeoutMillis = 100L, retransmitLimit = 1, windowSize = 4),
        chunkSizePolicy = ChunkSizePolicy(gattChunkSizeBytes = 1, l2capChunkSizeBytes = 1),
      )
    engine.startTransfer(
      transferId = "tx-1",
      recipientPeerId = PeerIdHex(value = "00112233"),
      priority = Priority.NORMAL,
      payload = byteArrayOf(0x01, 0x02, 0x03),
      preferL2cap = false,
      nowEpochMillis = 0L,
    )
    engine.acknowledge(transferId = "tx-1", chunkIndex = 0, nowEpochMillis = 10L)

    // Act
    val firstRetryWindow = engine.nextChunks(transferId = "tx-1")
    val secondRetryWindow = engine.nextChunks(transferId = "tx-1")
    val exhaustedRetryWindow = engine.nextChunks(transferId = "tx-1")
    val resumedProgress =
      engine.acknowledge(transferId = "tx-1", chunkIndex = 1, nowEpochMillis = 20L)
    val completion = engine.acknowledge(transferId = "tx-1", chunkIndex = 2, nowEpochMillis = 30L)

    // Assert
    assertEquals(
      expected = listOf(1, 2),
      actual = firstRetryWindow.map { chunk -> chunk.chunkIndex },
    )
    assertEquals(
      expected = listOf(1, 2),
      actual = secondRetryWindow.map { chunk -> chunk.chunkIndex },
    )
    assertEquals(expected = emptyList(), actual = exhaustedRetryWindow)
    val progress = assertIs<TransferEvent.Progress>(resumedProgress)
    assertEquals(expected = 2L, actual = progress.acknowledgedBytes)
    assertEquals(
      expected = TransferEvent.Complete(transferId = "tx-1", totalBytes = 3L),
      actual = completion,
    )
  }
}
