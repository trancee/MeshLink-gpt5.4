package ch.trancee.meshlink.transport

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class L2capRetrySchedulerTest {
  @Test
  public fun defaults_matchTheTransportBackoffContract(): Unit {
    // Arrange
    // Act
    val initialDelayMillis = L2capRetryScheduler.DEFAULT_INITIAL_DELAY_MILLIS
    val maxDelayMillis = L2capRetryScheduler.DEFAULT_MAX_DELAY_MILLIS
    val jitterRatio = L2capRetryScheduler.DEFAULT_JITTER_RATIO

    // Assert
    assertEquals(expected = 250L, actual = initialDelayMillis)
    assertEquals(expected = 8_000L, actual = maxDelayMillis)
    assertEquals(expected = 0.2, actual = jitterRatio)
  }

  @Test
  public fun delayMillisForAttempt_returnsTheInitialDelayForAttemptZero(): Unit {
    // Arrange
    val scheduler =
      L2capRetryScheduler(initialDelayMillis = 100, maxDelayMillis = 1_000, jitterRatio = 0.0)

    // Act
    val actual = scheduler.delayMillisForAttempt(attempt = 0)

    // Assert
    assertEquals(expected = 100L, actual = actual)
  }

  @Test
  public fun delayMillisForAttempt_doublesDelaysExponentiallyUntilTheCap(): Unit {
    // Arrange
    val scheduler =
      L2capRetryScheduler(initialDelayMillis = 100, maxDelayMillis = 500, jitterRatio = 0.0)

    // Act
    val first = scheduler.delayMillisForAttempt(attempt = 1)
    val second = scheduler.delayMillisForAttempt(attempt = 2)
    val third = scheduler.delayMillisForAttempt(attempt = 3)
    val fourth = scheduler.delayMillisForAttempt(attempt = 4)

    // Assert
    assertEquals(expected = 200L, actual = first)
    assertEquals(expected = 400L, actual = second)
    assertEquals(expected = 500L, actual = third)
    assertEquals(expected = 500L, actual = fourth)
  }

  @Test
  public fun delayMillisForAttempt_appliesNegativeJitterAtTheLowerBound(): Unit {
    // Arrange
    val scheduler =
      L2capRetryScheduler(
        initialDelayMillis = 100,
        maxDelayMillis = 1_000,
        jitterRatio = 0.2,
        randomUnitInterval = { 0.0 },
      )

    // Act
    val actual = scheduler.delayMillisForAttempt(attempt = 1)

    // Assert
    assertEquals(expected = 160L, actual = actual)
  }

  @Test
  public fun delayMillisForAttempt_appliesPositiveJitterAtTheUpperBound(): Unit {
    // Arrange
    val scheduler =
      L2capRetryScheduler(
        initialDelayMillis = 100,
        maxDelayMillis = 1_000,
        jitterRatio = 0.2,
        randomUnitInterval = { 1.0 },
      )

    // Act
    val actual = scheduler.delayMillisForAttempt(attempt = 1)

    // Assert
    assertEquals(expected = 240L, actual = actual)
  }

  @Test
  public fun delayMillisForAttempt_doesNotEvaluateRandomSourceWhenJitterIsDisabled(): Unit {
    // Arrange
    var randomEvaluated = false
    val scheduler =
      L2capRetryScheduler(
        initialDelayMillis = 100,
        maxDelayMillis = 1_000,
        jitterRatio = 0.0,
        randomUnitInterval = {
          randomEvaluated = true
          0.5
        },
      )

    // Act
    val actual = scheduler.delayMillisForAttempt(attempt = 2)

    // Assert
    assertEquals(expected = 400L, actual = actual)
    assertEquals(expected = false, actual = randomEvaluated)
  }

  @Test
  public fun delayMillisForAttempt_rejectsNegativeAttempts(): Unit {
    // Arrange
    val scheduler = L2capRetryScheduler()

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> { scheduler.delayMillisForAttempt(attempt = -1) }

    // Assert
    assertEquals(
      expected = "L2capRetryScheduler attempt must be greater than or equal to 0.",
      actual = error.message,
    )
  }

  @Test
  public fun delayMillisForAttempt_rejectsOutOfRangeRandomValues(): Unit {
    // Arrange
    val scheduler =
      L2capRetryScheduler(
        initialDelayMillis = 100,
        maxDelayMillis = 1_000,
        jitterRatio = 0.2,
        randomUnitInterval = { 1.5 },
      )

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> { scheduler.delayMillisForAttempt(attempt = 1) }

    // Assert
    assertEquals(
      expected = "L2capRetryScheduler randomUnitInterval must return a value between 0.0 and 1.0.",
      actual = error.message,
    )
  }

  @Test
  public fun delayMillisForAttempt_rejectsNegativeRandomValues(): Unit {
    // Arrange
    val scheduler =
      L2capRetryScheduler(
        initialDelayMillis = 100,
        maxDelayMillis = 1_000,
        jitterRatio = 0.2,
        randomUnitInterval = { -0.1 },
      )

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> { scheduler.delayMillisForAttempt(attempt = 1) }

    // Assert
    assertEquals(
      expected = "L2capRetryScheduler randomUnitInterval must return a value between 0.0 and 1.0.",
      actual = error.message,
    )
  }

  @Test
  public fun init_rejectsNonPositiveInitialDelays(): Unit {
    // Arrange
    val expectedMessage = "L2capRetryScheduler initialDelayMillis must be greater than 0."

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> { L2capRetryScheduler(initialDelayMillis = 0) }

    // Assert
    assertEquals(expected = expectedMessage, actual = error.message)
  }

  @Test
  public fun init_rejectsMaximumsSmallerThanTheInitialDelay(): Unit {
    // Arrange
    val expectedMessage =
      "L2capRetryScheduler maxDelayMillis must be greater than or equal to initialDelayMillis."

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> {
        L2capRetryScheduler(initialDelayMillis = 200, maxDelayMillis = 100)
      }

    // Assert
    assertEquals(expected = expectedMessage, actual = error.message)
  }

  @Test
  public fun init_rejectsJitterRatiosOutsideTheUnitInterval(): Unit {
    // Arrange
    val expectedMessage = "L2capRetryScheduler jitterRatio must be between 0.0 and 1.0."

    // Act
    val error = assertFailsWith<IllegalArgumentException> { L2capRetryScheduler(jitterRatio = 1.1) }

    // Assert
    assertEquals(expected = expectedMessage, actual = error.message)
  }

  @Test
  public fun init_rejectsNegativeJitterRatios(): Unit {
    // Arrange
    val expectedMessage = "L2capRetryScheduler jitterRatio must be between 0.0 and 1.0."

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> { L2capRetryScheduler(jitterRatio = -0.1) }

    // Assert
    assertEquals(expected = expectedMessage, actual = error.message)
  }
}
