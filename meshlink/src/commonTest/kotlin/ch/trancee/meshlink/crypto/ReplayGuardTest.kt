package ch.trancee.meshlink.crypto

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

public class ReplayGuardTest {
    @Test
    public fun checkAndMark_acceptsFirstNonce(): Unit {
        // Arrange
        val guard = ReplayGuard()

        // Act
        val actual: Boolean = guard.checkAndMark(nonce = 10L)

        // Assert
        assertTrue(
            actual = actual,
            message = "ReplayGuard should accept the first nonce it sees",
        )
    }

    @Test
    public fun checkAndMark_rejectsDuplicateNonce(): Unit {
        // Arrange
        val guard = ReplayGuard()
        guard.checkAndMark(nonce = 10L)

        // Act
        val actual: Boolean = guard.checkAndMark(nonce = 10L)

        // Assert
        assertFalse(
            actual = actual,
            message = "ReplayGuard should reject duplicate nonces",
        )
    }

    @Test
    public fun checkAndMark_acceptsOutOfOrderNonceWithinWindowOnce(): Unit {
        // Arrange
        val guard = ReplayGuard()
        guard.checkAndMark(nonce = 10L)
        guard.checkAndMark(nonce = 12L)

        // Act
        val actual: Boolean = guard.checkAndMark(nonce = 11L)

        // Assert
        assertTrue(
            actual = actual,
            message = "ReplayGuard should accept unseen nonces that arrive out of order within the active window",
        )
    }

    @Test
    public fun checkAndMark_rejectsNonceOutsideWindowAfterAdvance(): Unit {
        // Arrange
        val guard = ReplayGuard()
        guard.checkAndMark(nonce = 0L)
        guard.checkAndMark(nonce = 64L)

        // Act
        val actual: Boolean = guard.checkAndMark(nonce = 0L)

        // Assert
        assertFalse(
            actual = actual,
            message = "ReplayGuard should reject nonces that fall outside the 64-entry replay window",
        )
    }

    @Test
    public fun checkAndMark_acceptsFarAheadNonceAndResetsWindow(): Unit {
        // Arrange
        val guard = ReplayGuard()
        guard.checkAndMark(nonce = 3L)

        // Act
        val actual: Boolean = guard.checkAndMark(nonce = 100L)

        // Assert
        assertTrue(
            actual = actual,
            message = "ReplayGuard should accept far-ahead nonces and advance the sliding window",
        )
    }

    @Test
    public fun checkAndMark_rejectsNegativeNonce(): Unit {
        // Arrange
        val guard = ReplayGuard()

        // Act
        val actual: Boolean = guard.checkAndMark(nonce = -1L)

        // Assert
        assertFalse(
            actual = actual,
            message = "ReplayGuard should reject negative nonces",
        )
    }
}
