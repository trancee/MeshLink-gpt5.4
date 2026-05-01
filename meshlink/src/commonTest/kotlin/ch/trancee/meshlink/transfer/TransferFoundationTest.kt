package ch.trancee.meshlink.transfer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class TransferFoundationTest {
    @Test
    public fun transferConfig_defaultExposesExpectedTimeoutRetryAndWindowValues(): Unit {
        // Arrange
        // Act
        val actual = TransferConfig.default()

        // Assert
        assertEquals(expected = 5_000L, actual = actual.timeoutMillis)
        assertEquals(expected = 3, actual = actual.retransmitLimit)
        assertEquals(expected = 8, actual = actual.windowSize)
    }

    @Test
    public fun transferConfig_rejectsInvalidValues(): Unit {
        // Arrange
        // Act
        val timeoutError = assertFailsWith<IllegalArgumentException> {
            TransferConfig(timeoutMillis = 0L, retransmitLimit = 0, windowSize = 1)
        }
        val retryError = assertFailsWith<IllegalArgumentException> {
            TransferConfig(timeoutMillis = 1L, retransmitLimit = -1, windowSize = 1)
        }
        val windowError = assertFailsWith<IllegalArgumentException> {
            TransferConfig(timeoutMillis = 1L, retransmitLimit = 0, windowSize = 0)
        }

        // Assert
        assertEquals(expected = "TransferConfig timeoutMillis must be greater than 0.", actual = timeoutError.message)
        assertEquals(expected = "TransferConfig retransmitLimit must be greater than or equal to 0.", actual = retryError.message)
        assertEquals(expected = "TransferConfig windowSize must be greater than 0.", actual = windowError.message)
    }

    @Test
    public fun chunkSizePolicy_defaultExposesGattAndL2capChunkSizes(): Unit {
        // Arrange
        // Act
        val actual = ChunkSizePolicy.default()

        // Assert
        assertEquals(expected = 244, actual = actual.gattChunkSizeBytes)
        assertEquals(expected = 1024, actual = actual.l2capChunkSizeBytes)
        assertEquals(expected = 244, actual = actual.sizeFor(preferL2cap = false))
        assertEquals(expected = 1024, actual = actual.sizeFor(preferL2cap = true))
    }

    @Test
    public fun chunkSizePolicy_rejectsInvalidChunkSizes(): Unit {
        // Arrange
        // Act
        val gattError = assertFailsWith<IllegalArgumentException> {
            ChunkSizePolicy(gattChunkSizeBytes = 0, l2capChunkSizeBytes = 1)
        }
        val l2capError = assertFailsWith<IllegalArgumentException> {
            ChunkSizePolicy(gattChunkSizeBytes = 1, l2capChunkSizeBytes = 0)
        }

        // Assert
        assertEquals(expected = "ChunkSizePolicy gattChunkSizeBytes must be greater than 0.", actual = gattError.message)
        assertEquals(expected = "ChunkSizePolicy l2capChunkSizeBytes must be greater than 0.", actual = l2capError.message)
    }

    @Test
    public fun transferEvents_andPriorityTypes_retainTheirConstructorValues(): Unit {
        // Arrange
        // Act
        val started: TransferEvent = TransferEvent.Started(transferId = "tx-1", priority = Priority.HIGH)
        val progress: TransferEvent = TransferEvent.Progress(transferId = "tx-1", acknowledgedBytes = 128L, totalBytes = 256L)
        val complete: TransferEvent = TransferEvent.Complete(transferId = "tx-1", totalBytes = 256L)
        val failed: TransferEvent = TransferEvent.Failed(transferId = "tx-1", reason = FailureReason.TIMEOUT)

        // Assert
        assertEquals(expected = TransferEvent.Started(transferId = "tx-1", priority = Priority.HIGH), actual = started)
        assertEquals(expected = TransferEvent.Progress(transferId = "tx-1", acknowledgedBytes = 128L, totalBytes = 256L), actual = progress)
        assertEquals(expected = TransferEvent.Complete(transferId = "tx-1", totalBytes = 256L), actual = complete)
        assertEquals(expected = TransferEvent.Failed(transferId = "tx-1", reason = FailureReason.TIMEOUT), actual = failed)
    }
}
