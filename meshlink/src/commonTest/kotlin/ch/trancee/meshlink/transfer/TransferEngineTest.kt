package ch.trancee.meshlink.transfer

import ch.trancee.meshlink.api.DiagnosticCode
import ch.trancee.meshlink.api.DiagnosticSink
import ch.trancee.meshlink.api.PeerIdHex
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

public class TransferEngineTest {
  @Test
  public fun startTransfer_schedulesAndChunksPayloadsAccordingToTheSelectedPolicy(): Unit {
    // Arrange
    val engine =
      TransferEngine(
        config = TransferConfig(timeoutMillis = 100L, retransmitLimit = 1, windowSize = 4),
        chunkSizePolicy = ChunkSizePolicy(gattChunkSizeBytes = 2, l2capChunkSizeBytes = 4),
      )

    // Act
    val started =
      engine.startTransfer(
        transferId = "tx-1",
        recipientPeerId = PeerIdHex(value = "00112233"),
        priority = Priority.HIGH,
        payload = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05),
        preferL2cap = false,
        nowEpochMillis = 0L,
      )
    val scheduledTransferId = engine.nextScheduledTransferId()
    val actualChunks = engine.nextChunks(transferId = "tx-1")

    // Assert
    assertEquals(
      expected = TransferEvent.Started(transferId = "tx-1", priority = Priority.HIGH),
      actual = started,
    )
    assertEquals(expected = "tx-1", actual = scheduledTransferId)
    assertEquals(expected = 3, actual = actualChunks.size)
    assertContentEquals(expected = byteArrayOf(0x01, 0x02), actual = actualChunks[0].payload)
    assertContentEquals(expected = byteArrayOf(0x03, 0x04), actual = actualChunks[1].payload)
    assertContentEquals(expected = byteArrayOf(0x05), actual = actualChunks[2].payload)
  }

  @Test
  public fun nextChunks_usesPacingFeedbackToReduceTheWindowAfterAcknowledgements(): Unit {
    // Arrange
    val engine =
      TransferEngine(
        config = TransferConfig(timeoutMillis = 100L, retransmitLimit = 2, windowSize = 4),
        chunkSizePolicy = ChunkSizePolicy(gattChunkSizeBytes = 1, l2capChunkSizeBytes = 1),
      )
    engine.startTransfer(
      transferId = "paced",
      recipientPeerId = PeerIdHex(value = "00112233"),
      priority = Priority.NORMAL,
      payload = byteArrayOf(0x01, 0x02, 0x03),
      preferL2cap = false,
      nowEpochMillis = 0L,
    )
    engine.acknowledge(transferId = "paced", chunkIndex = 0, nowEpochMillis = 10L)
    engine.acknowledge(transferId = "paced", chunkIndex = 1, nowEpochMillis = 30L)

    // Act
    val actual = engine.nextChunks(transferId = "paced")

    // Assert
    assertEquals(
      expected = listOf(2),
      actual = actual.map { chunk -> chunk.chunkIndex },
      message = "TransferEngine should feed acknowledgement pacing back into chunk selection.",
    )
    assertEquals(expected = 20L, actual = engine.recommendedDelayMillis(transferId = "paced"))
  }

  @Test
  public fun acknowledge_retransmitsOnlyMissingChunksAndCompletesTheTransfer(): Unit {
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

    // Act
    val firstProgress =
      engine.acknowledge(transferId = "tx-1", chunkIndex = 0, nowEpochMillis = 10L)
    val secondProgress =
      engine.acknowledge(transferId = "tx-1", chunkIndex = 2, nowEpochMillis = 30L)
    val recommendedDelayBeforeCompletion = engine.recommendedDelayMillis(transferId = "tx-1")
    val retransmitChunks = engine.nextChunks(transferId = "tx-1")
    val completion = engine.acknowledge(transferId = "tx-1", chunkIndex = 1, nowEpochMillis = 50L)
    val recommendedDelayAfterCompletion = engine.recommendedDelayMillis(transferId = "tx-1")

    // Assert
    assertEquals(
      expected =
        TransferEvent.Progress(transferId = "tx-1", acknowledgedBytes = 1L, totalBytes = 3L),
      actual = firstProgress,
    )
    assertEquals(
      expected =
        TransferEvent.Progress(transferId = "tx-1", acknowledgedBytes = 2L, totalBytes = 3L),
      actual = secondProgress,
    )
    assertEquals(expected = 20L, actual = recommendedDelayBeforeCompletion)
    assertEquals(expected = listOf(1), actual = retransmitChunks.map { chunk -> chunk.chunkIndex })
    assertEquals(
      expected = TransferEvent.Complete(transferId = "tx-1", totalBytes = 3L),
      actual = completion,
    )
    assertEquals(expected = null, actual = recommendedDelayAfterCompletion)
    assertEquals(expected = 0, actual = engine.pendingTransfers())
  }

  @Test
  public fun cancelAndTimeout_releaseTransfersAndReportFailures(): Unit {
    // Arrange
    val engine =
      TransferEngine(
        config = TransferConfig(timeoutMillis = 100L, retransmitLimit = 1, windowSize = 1),
        chunkSizePolicy = ChunkSizePolicy.default(),
      )
    engine.startTransfer(
      transferId = "cancel-me",
      recipientPeerId = PeerIdHex(value = "00112233"),
      priority = Priority.NORMAL,
      payload = byteArrayOf(0x01),
      preferL2cap = false,
      nowEpochMillis = 0L,
    )
    engine.startTransfer(
      transferId = "timeout-me",
      recipientPeerId = PeerIdHex(value = "44556677"),
      priority = Priority.NORMAL,
      payload = byteArrayOf(0x02),
      preferL2cap = false,
      nowEpochMillis = 0L,
    )

    // Act
    val cancelled = engine.cancel(transferId = "cancel-me")
    val notYetTimedOut = engine.failTimedOut(nowEpochMillis = 99L)
    val timedOut = engine.failTimedOut(nowEpochMillis = 100L)
    val noLongerTimedOut = engine.failTimedOut(nowEpochMillis = 101L)
    val missingCancel = engine.cancel(transferId = "missing")

    // Assert
    assertEquals(
      expected = TransferEvent.Failed(transferId = "cancel-me", reason = FailureReason.CANCELLED),
      actual = cancelled,
    )
    assertEquals(expected = emptyList(), actual = notYetTimedOut)
    assertEquals(
      expected =
        listOf(TransferEvent.Failed(transferId = "timeout-me", reason = FailureReason.TIMEOUT)),
      actual = timedOut,
    )
    assertEquals(expected = emptyList(), actual = noLongerTimedOut)
    assertEquals(expected = null, actual = missingCancel)
    assertEquals(expected = 0, actual = engine.pendingTransfers())
  }

  @Test
  public fun transferLifecycle_emitsExistingDiagnosticCodesForStartedProgressCompletedAndFailedPaths():
    Unit {
    // Arrange
    val diagnosticSink = DiagnosticSink.create(bufferSize = 8, clock = { 1L })
    val engine =
      TransferEngine(
        config = TransferConfig(timeoutMillis = 100L, retransmitLimit = 1, windowSize = 1),
        chunkSizePolicy = ChunkSizePolicy(gattChunkSizeBytes = 1, l2capChunkSizeBytes = 1),
        diagnosticSink = diagnosticSink,
      )

    // Act
    engine.startTransfer(
      transferId = "complete-me",
      recipientPeerId = PeerIdHex(value = "00112233"),
      priority = Priority.NORMAL,
      payload = byteArrayOf(0x01),
      preferL2cap = false,
      nowEpochMillis = 0L,
    )
    engine.acknowledge(transferId = "complete-me", chunkIndex = 0, nowEpochMillis = 1L)
    engine.startTransfer(
      transferId = "cancel-me",
      recipientPeerId = PeerIdHex(value = "44556677"),
      priority = Priority.NORMAL,
      payload = byteArrayOf(0x02),
      preferL2cap = false,
      nowEpochMillis = 0L,
    )
    engine.cancel(transferId = "cancel-me")
    val actual = diagnosticSink.diagnosticEvents.replayCache.map { event -> event.code }

    // Assert
    assertEquals(
      expected =
        listOf(
          DiagnosticCode.TRANSFER_STARTED,
          DiagnosticCode.TRANSFER_COMPLETED,
          DiagnosticCode.TRANSFER_STARTED,
          DiagnosticCode.TRANSFER_FAILED,
        ),
      actual = actual,
    )
  }

  @Test
  public fun receiveChunk_reassemblesInboundTransfersOnceAllChunksArrive(): Unit {
    // Arrange
    val engine =
      TransferEngine(config = TransferConfig.default(), chunkSizePolicy = ChunkSizePolicy.default())

    // Act
    val first =
      engine.receiveChunk(
        transferId = "rx-1",
        chunkIndex = 0,
        totalChunks = 2,
        payload = byteArrayOf(0x01, 0x02),
      )
    val second =
      engine.receiveChunk(
        transferId = "rx-1",
        chunkIndex = 1,
        totalChunks = 2,
        payload = byteArrayOf(0x03),
      )

    // Assert
    assertNull(first)
    assertContentEquals(expected = byteArrayOf(0x01, 0x02, 0x03), actual = second)
  }

  @Test
  public fun invalidInputs_andMissingTransfers_areRejected(): Unit {
    // Arrange
    val engine =
      TransferEngine(config = TransferConfig.default(), chunkSizePolicy = ChunkSizePolicy.default())
    engine.startTransfer(
      transferId = "tx-1",
      recipientPeerId = PeerIdHex(value = "00112233"),
      priority = Priority.NORMAL,
      payload = byteArrayOf(0x01),
      preferL2cap = false,
      nowEpochMillis = 0L,
    )

    // Act
    val duplicateTransferError =
      assertFailsWith<IllegalArgumentException> {
        engine.startTransfer(
          transferId = "tx-1",
          recipientPeerId = PeerIdHex(value = "00112233"),
          priority = Priority.NORMAL,
          payload = byteArrayOf(0x01),
          preferL2cap = false,
          nowEpochMillis = 1L,
        )
      }
    val missingAcknowledge =
      engine.acknowledge(transferId = "missing", chunkIndex = 0, nowEpochMillis = 0L)
    val missingDelay = engine.recommendedDelayMillis(transferId = "missing")
    val negativeTimestampError =
      assertFailsWith<IllegalArgumentException> {
        engine.startTransfer(
          transferId = "tx-2",
          recipientPeerId = PeerIdHex(value = "00112233"),
          priority = Priority.NORMAL,
          payload = byteArrayOf(0x01),
          preferL2cap = false,
          nowEpochMillis = -1L,
        )
      }
    val missingTransferError =
      assertFailsWith<IllegalArgumentException> { engine.nextChunks(transferId = "missing") }
    val negativeAcknowledgeTimestampError =
      assertFailsWith<IllegalArgumentException> {
        engine.acknowledge(transferId = "tx-1", chunkIndex = 0, nowEpochMillis = -1L)
      }
    val negativeTimeoutTimestampError =
      assertFailsWith<IllegalArgumentException> { engine.failTimedOut(nowEpochMillis = -1L) }
    val invalidReceiveChunkIndexError =
      assertFailsWith<IllegalArgumentException> {
        engine.receiveChunk(
          transferId = "rx",
          chunkIndex = 2,
          totalChunks = 2,
          payload = byteArrayOf(0x01),
        )
      }
    val negativeReceiveChunkIndexError =
      assertFailsWith<IllegalArgumentException> {
        engine.receiveChunk(
          transferId = "rx",
          chunkIndex = -1,
          totalChunks = 2,
          payload = byteArrayOf(0x01),
        )
      }
    val invalidReceiveChunkCountError =
      assertFailsWith<IllegalArgumentException> {
        engine.receiveChunk(
          transferId = "rx",
          chunkIndex = 0,
          totalChunks = 0,
          payload = byteArrayOf(0x01),
        )
      }

    // Assert
    assertEquals(
      expected = "TransferEngine already has an active transfer for tx-1.",
      actual = duplicateTransferError.message,
    )
    assertEquals(expected = null, actual = missingAcknowledge)
    assertEquals(expected = null, actual = missingDelay)
    assertEquals(
      expected = "TransferEngine nowEpochMillis must be greater than or equal to 0.",
      actual = negativeTimestampError.message,
    )
    assertEquals(
      expected = "TransferEngine has no active transfer for missing.",
      actual = missingTransferError.message,
    )
    assertEquals(
      expected = "TransferEngine nowEpochMillis must be greater than or equal to 0.",
      actual = negativeAcknowledgeTimestampError.message,
    )
    assertEquals(
      expected = "TransferEngine nowEpochMillis must be greater than or equal to 0.",
      actual = negativeTimeoutTimestampError.message,
    )
    assertEquals(
      expected = "TransferEngine chunkIndex must be between 0 and 1.",
      actual = invalidReceiveChunkIndexError.message,
    )
    assertEquals(
      expected = "TransferEngine chunkIndex must be between 0 and 1.",
      actual = negativeReceiveChunkIndexError.message,
    )
    assertEquals(
      expected = "TransferEngine totalChunks must be greater than 0.",
      actual = invalidReceiveChunkCountError.message,
    )
  }
}
