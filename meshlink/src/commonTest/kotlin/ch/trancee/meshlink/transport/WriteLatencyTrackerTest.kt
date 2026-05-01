package ch.trancee.meshlink.transport

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class WriteLatencyTrackerTest {
    @Test
    public fun defaultWindowSize_matchesTheTrackerContract(): Unit {
        // Arrange
        val expected = 16

        // Act
        val actual = WriteLatencyTracker.DEFAULT_WINDOW_SIZE

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    public fun snapshot_returnsZeroValuesWhenNoSamplesExist(): Unit {
        // Arrange
        val tracker = WriteLatencyTracker()

        // Act
        val actual = tracker.snapshot()

        // Assert
        assertEquals(expected = 0, actual = actual.sampleCount)
        assertEquals(expected = 0L, actual = actual.minimumMillis)
        assertEquals(expected = 0L, actual = actual.maximumMillis)
        assertEquals(expected = 0.0, actual = actual.averageMillis)
    }

    @Test
    public fun record_updatesTheLatencySnapshot(): Unit {
        // Arrange
        val tracker = WriteLatencyTracker(windowSize = 4)

        // Act
        tracker.record(durationMillis = 10)
        tracker.record(durationMillis = 40)
        tracker.record(durationMillis = 20)
        val actual = tracker.snapshot()

        // Assert
        assertEquals(expected = 3, actual = actual.sampleCount)
        assertEquals(expected = 10L, actual = actual.minimumMillis)
        assertEquals(expected = 40L, actual = actual.maximumMillis)
        assertEquals(expected = 70.0 / 3.0, actual = actual.averageMillis)
    }

    @Test
    public fun record_evictsTheOldestSampleWhenTheWindowIsFull(): Unit {
        // Arrange
        val tracker = WriteLatencyTracker(windowSize = 2)

        // Act
        tracker.record(durationMillis = 10)
        tracker.record(durationMillis = 20)
        tracker.record(durationMillis = 30)
        val actual = tracker.snapshot()

        // Assert
        assertEquals(expected = 2, actual = actual.sampleCount)
        assertEquals(expected = 20L, actual = actual.minimumMillis)
        assertEquals(expected = 30L, actual = actual.maximumMillis)
        assertEquals(expected = 25.0, actual = actual.averageMillis)
    }

    @Test
    public fun record_rejectsNegativeDurations(): Unit {
        // Arrange
        val tracker = WriteLatencyTracker()

        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            tracker.record(durationMillis = -1)
        }

        // Assert
        assertEquals(
            expected = "WriteLatencyTracker durationMillis must be greater than or equal to 0.",
            actual = error.message,
        )
    }

    @Test
    public fun init_rejectsNonPositiveWindowSizes(): Unit {
        // Arrange
        val expectedMessage = "WriteLatencyTracker windowSize must be greater than 0."

        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            WriteLatencyTracker(windowSize = 0)
        }

        // Assert
        assertEquals(expected = expectedMessage, actual = error.message)
    }
}
