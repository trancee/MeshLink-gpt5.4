package ch.trancee.meshlink.transfer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class TransferSupportTest {
    @Test
    public fun sackTracker_tracksAcknowledgedMissingAndContiguousChunks(): Unit {
        // Arrange
        val sackTracker = SackTracker(totalChunks = 4)

        // Act
        sackTracker.acknowledge(chunkIndex = 0)
        sackTracker.acknowledge(chunkIndex = 2)
        val acknowledged = sackTracker.acknowledgedChunks()
        val missing = sackTracker.missingChunks()
        val contiguousBeforeGap = sackTracker.highestContiguousAcknowledgedChunkIndex()
        val firstAcknowledged = sackTracker.isAcknowledged(chunkIndex = 0)
        val missingAcknowledged = sackTracker.isAcknowledged(chunkIndex = 1)
        val completeBeforeFinalAcks = sackTracker.isComplete()
        sackTracker.acknowledge(chunkIndex = 1)
        sackTracker.acknowledge(chunkIndex = 3)
        val contiguousAfterGap = sackTracker.highestContiguousAcknowledgedChunkIndex()
        val completeAfterFinalAcks = sackTracker.isComplete()

        // Assert
        assertEquals(expected = setOf(0, 2), actual = acknowledged)
        assertEquals(expected = listOf(1, 3), actual = missing)
        assertEquals(expected = 0, actual = contiguousBeforeGap)
        assertEquals(expected = true, actual = firstAcknowledged)
        assertEquals(expected = false, actual = missingAcknowledged)
        assertEquals(expected = false, actual = completeBeforeFinalAcks)
        assertEquals(expected = 3, actual = contiguousAfterGap)
        assertEquals(expected = true, actual = completeAfterFinalAcks)
    }

    @Test
    public fun sackTracker_rejectsInvalidConstructionAndChunkIndices(): Unit {
        // Arrange
        // Act
        val initError = assertFailsWith<IllegalArgumentException> {
            SackTracker(totalChunks = 0)
        }
        val sackTracker = SackTracker(totalChunks = 2)
        val acknowledgeTooLargeError = assertFailsWith<IllegalArgumentException> {
            sackTracker.acknowledge(chunkIndex = 2)
        }
        val acknowledgeNegativeError = assertFailsWith<IllegalArgumentException> {
            sackTracker.acknowledge(chunkIndex = -1)
        }
        val isAcknowledgedNegativeError = assertFailsWith<IllegalArgumentException> {
            sackTracker.isAcknowledged(chunkIndex = -1)
        }
        val isAcknowledgedTooLargeError = assertFailsWith<IllegalArgumentException> {
            sackTracker.isAcknowledged(chunkIndex = 2)
        }

        // Assert
        assertEquals(expected = "SackTracker totalChunks must be greater than 0.", actual = initError.message)
        assertEquals(expected = "SackTracker chunkIndex must be between 0 and 1.", actual = acknowledgeTooLargeError.message)
        assertEquals(expected = "SackTracker chunkIndex must be between 0 and 1.", actual = acknowledgeNegativeError.message)
        assertEquals(expected = "SackTracker chunkIndex must be between 0 and 1.", actual = isAcknowledgedNegativeError.message)
        assertEquals(expected = "SackTracker chunkIndex must be between 0 and 1.", actual = isAcknowledgedTooLargeError.message)
    }

    @Test
    public fun resumeCalculator_usesTheHighestContiguousSackOffset(): Unit {
        // Arrange
        val sackTracker = SackTracker(totalChunks = 4)
        sackTracker.acknowledge(chunkIndex = 0)
        sackTracker.acknowledge(chunkIndex = 1)
        sackTracker.acknowledge(chunkIndex = 3)

        // Act
        val actual = ResumeCalculator.resumeOffsetBytes(
            sackTracker = sackTracker,
            chunkSizeBytes = 100,
            totalBytes = 350,
        )

        // Assert
        assertEquals(expected = 200, actual = actual)
    }

    @Test
    public fun resumeCalculator_returnsZeroWithoutAContiguousPrefixAndClipsToTotalBytes(): Unit {
        // Arrange
        val emptyPrefixTracker = SackTracker(totalChunks = 2)
        emptyPrefixTracker.acknowledge(chunkIndex = 1)
        val clippedTracker = SackTracker(totalChunks = 3)
        clippedTracker.acknowledge(chunkIndex = 0)
        clippedTracker.acknowledge(chunkIndex = 1)

        // Act
        val zeroOffset = ResumeCalculator.resumeOffsetBytes(
            sackTracker = emptyPrefixTracker,
            chunkSizeBytes = 100,
            totalBytes = 200,
        )
        val clippedOffset = ResumeCalculator.resumeOffsetBytes(
            sackTracker = clippedTracker,
            chunkSizeBytes = 100,
            totalBytes = 150,
        )

        // Assert
        assertEquals(expected = 0, actual = zeroOffset)
        assertEquals(expected = 150, actual = clippedOffset)
    }

    @Test
    public fun resumeCalculator_rejectsInvalidChunkSizesAndTotalBytes(): Unit {
        // Arrange
        val sackTracker = SackTracker(totalChunks = 1)

        // Act
        val chunkSizeError = assertFailsWith<IllegalArgumentException> {
            ResumeCalculator.resumeOffsetBytes(sackTracker = sackTracker, chunkSizeBytes = 0, totalBytes = 0)
        }
        val totalBytesError = assertFailsWith<IllegalArgumentException> {
            ResumeCalculator.resumeOffsetBytes(sackTracker = sackTracker, chunkSizeBytes = 1, totalBytes = -1)
        }

        // Assert
        assertEquals(expected = "ResumeCalculator chunkSizeBytes must be greater than 0.", actual = chunkSizeError.message)
        assertEquals(expected = "ResumeCalculator totalBytes must be greater than or equal to 0.", actual = totalBytesError.message)
    }

    @Test
    public fun observationRateController_tracksAverageAcknowledgementIntervals(): Unit {
        // Arrange
        val controller = ObservationRateController(windowSize = 2)

        // Act
        controller.recordAcknowledgement(timestampMillis = 10L)
        controller.recordAcknowledgement(timestampMillis = 30L)
        controller.recordAcknowledgement(timestampMillis = 70L)
        controller.recordAcknowledgement(timestampMillis = 90L)
        val actual = controller.recommendedDelayMillis()

        // Assert
        assertEquals(expected = 30L, actual = actual)
    }

    @Test
    public fun observationRateController_returnsZeroBeforeTwoSamplesAndRejectsInvalidInputs(): Unit {
        // Arrange
        val controller = ObservationRateController(windowSize = 1)

        // Act
        val initialDelay = controller.recommendedDelayMillis()
        controller.recordAcknowledgement(timestampMillis = 5L)
        val singleSampleDelay = controller.recommendedDelayMillis()
        val initError = assertFailsWith<IllegalArgumentException> {
            ObservationRateController(windowSize = 0)
        }
        val timestampError = assertFailsWith<IllegalArgumentException> {
            controller.recordAcknowledgement(timestampMillis = -1L)
        }
        val decreasingTimestampError = assertFailsWith<IllegalArgumentException> {
            controller.recordAcknowledgement(timestampMillis = 4L)
        }

        // Assert
        assertEquals(expected = 0L, actual = initialDelay)
        assertEquals(expected = 0L, actual = singleSampleDelay)
        assertEquals(expected = "ObservationRateController windowSize must be greater than 0.", actual = initError.message)
        assertEquals(expected = "ObservationRateController timestampMillis must be greater than or equal to 0.", actual = timestampError.message)
        assertEquals(expected = "ObservationRateController timestamps must be non-decreasing.", actual = decreasingTimestampError.message)
    }

    @Test
    public fun transferScheduler_ordersQueuedTransfersByPriorityAndInsertionOrder(): Unit {
        // Arrange
        val scheduler = TransferScheduler()

        // Act
        scheduler.enqueue(transferId = "low", priority = Priority.LOW)
        scheduler.enqueue(transferId = "normal", priority = Priority.NORMAL)
        scheduler.enqueue(transferId = "high-1", priority = Priority.HIGH)
        scheduler.enqueue(transferId = "high-2", priority = Priority.HIGH)
        val first = scheduler.dequeue()
        val second = scheduler.dequeue()
        val third = scheduler.dequeue()
        val fourth = scheduler.dequeue()
        val fifth = scheduler.dequeue()

        // Assert
        assertEquals(expected = "high-1", actual = first)
        assertEquals(expected = "high-2", actual = second)
        assertEquals(expected = "normal", actual = third)
        assertEquals(expected = "low", actual = fourth)
        assertEquals(expected = null, actual = fifth)
        assertEquals(expected = 0, actual = scheduler.size())
    }

    @Test
    public fun transferScheduler_requeuesExistingTransfersAndRejectsBlankIds(): Unit {
        // Arrange
        val scheduler = TransferScheduler()
        scheduler.enqueue(transferId = "transfer-1", priority = Priority.LOW)

        // Act
        scheduler.enqueue(transferId = "transfer-1", priority = Priority.HIGH)
        val actual = scheduler.dequeue()
        val error = assertFailsWith<IllegalArgumentException> {
            scheduler.enqueue(transferId = "   ", priority = Priority.NORMAL)
        }

        // Assert
        assertEquals(expected = "transfer-1", actual = actual)
        assertEquals(expected = "TransferScheduler transferId must not be blank.", actual = error.message)
    }
}
