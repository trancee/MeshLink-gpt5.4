package ch.trancee.meshlink.transfer

import ch.trancee.meshlink.api.PeerIdHex
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class TransferSessionTest {
  @Test
  public fun nextChunks_splitsPayloadsAccordingToTheConfiguredChunkSize(): Unit {
    // Arrange
    val session =
      TransferSession(
        transferId = "tx-1",
        recipientPeerId = PeerIdHex(value = "00112233"),
        priority = Priority.NORMAL,
        payload = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05),
        chunkSizeBytes = 2,
      )

    // Act
    val actual = session.nextChunks(windowSize = 3)
    val totalChunks = session.totalChunks()

    // Assert
    assertEquals(expected = 3, actual = totalChunks)
    assertEquals(expected = 3, actual = actual.size)
    assertEquals(expected = 0, actual = actual[0].chunkIndex)
    assertContentEquals(expected = byteArrayOf(0x01, 0x02), actual = actual[0].payload)
    assertEquals(expected = 1, actual = actual[1].chunkIndex)
    assertContentEquals(expected = byteArrayOf(0x03, 0x04), actual = actual[1].payload)
    assertEquals(expected = 2, actual = actual[2].chunkIndex)
    assertContentEquals(expected = byteArrayOf(0x05), actual = actual[2].payload)
  }

  @Test
  public fun acknowledge_emitsProgressUntilAllChunksAreAcknowledged(): Unit {
    // Arrange
    val session =
      TransferSession(
        transferId = "tx-1",
        recipientPeerId = PeerIdHex(value = "00112233"),
        priority = Priority.NORMAL,
        payload = byteArrayOf(0x01, 0x02, 0x03),
        chunkSizeBytes = 1,
      )

    // Act
    val first = session.acknowledge(chunkIndex = 0)
    val second = session.acknowledge(chunkIndex = 1)
    val third = session.acknowledge(chunkIndex = 2)

    // Assert
    assertEquals(
      expected =
        TransferEvent.Progress(transferId = "tx-1", acknowledgedBytes = 1L, totalBytes = 3L),
      actual = first,
    )
    assertEquals(
      expected =
        TransferEvent.Progress(transferId = "tx-1", acknowledgedBytes = 2L, totalBytes = 3L),
      actual = second,
    )
    assertEquals(
      expected = TransferEvent.Complete(transferId = "tx-1", totalBytes = 3L),
      actual = third,
    )
    assertEquals(expected = true, actual = session.isComplete())
  }

  @Test
  public fun resumeOffsetBytes_usesTheContiguousAcknowledgedPrefix(): Unit {
    // Arrange
    val session =
      TransferSession(
        transferId = "tx-1",
        recipientPeerId = PeerIdHex(value = "00112233"),
        priority = Priority.HIGH,
        payload = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05),
        chunkSizeBytes = 2,
      )
    session.acknowledge(chunkIndex = 0)
    session.acknowledge(chunkIndex = 2)

    // Act
    val actual = session.resumeOffsetBytes()

    // Assert
    assertEquals(expected = 2, actual = actual)
  }

  @Test
  public fun cancel_returnsACancelledTransferEvent(): Unit {
    // Arrange
    val session =
      TransferSession(
        transferId = "tx-1",
        recipientPeerId = PeerIdHex(value = "00112233"),
        priority = Priority.LOW,
        payload = byteArrayOf(0x01),
        chunkSizeBytes = 1,
      )

    // Act
    val actual = session.cancel()

    // Assert
    assertEquals(
      expected = TransferEvent.Failed(transferId = "tx-1", reason = FailureReason.CANCELLED),
      actual = actual,
    )
  }

  @Test
  public fun invalidInputs_areRejected(): Unit {
    // Arrange
    val recipientPeerId = PeerIdHex(value = "00112233")

    // Act
    val idError =
      assertFailsWith<IllegalArgumentException> {
        TransferSession(
          transferId = "   ",
          recipientPeerId = recipientPeerId,
          priority = Priority.NORMAL,
          payload = byteArrayOf(0x01),
          chunkSizeBytes = 1,
        )
      }
    val payloadError =
      assertFailsWith<IllegalArgumentException> {
        TransferSession(
          transferId = "tx-1",
          recipientPeerId = recipientPeerId,
          priority = Priority.NORMAL,
          payload = byteArrayOf(),
          chunkSizeBytes = 1,
        )
      }
    val chunkSizeError =
      assertFailsWith<IllegalArgumentException> {
        TransferSession(
          transferId = "tx-1",
          recipientPeerId = recipientPeerId,
          priority = Priority.NORMAL,
          payload = byteArrayOf(0x01),
          chunkSizeBytes = 0,
        )
      }
    val session =
      TransferSession(
        transferId = "tx-2",
        recipientPeerId = recipientPeerId,
        priority = Priority.NORMAL,
        payload = byteArrayOf(0x01),
        chunkSizeBytes = 1,
      )
    val windowSizeError =
      assertFailsWith<IllegalArgumentException> { session.nextChunks(windowSize = 0) }

    // Assert
    assertEquals(
      expected = "TransferSession transferId must not be blank.",
      actual = idError.message,
    )
    assertEquals(
      expected = "TransferSession payload must not be empty.",
      actual = payloadError.message,
    )
    assertEquals(
      expected = "TransferSession chunkSizeBytes must be greater than 0.",
      actual = chunkSizeError.message,
    )
    assertEquals(
      expected = "TransferSession windowSize must be greater than 0.",
      actual = windowSizeError.message,
    )
  }
}
