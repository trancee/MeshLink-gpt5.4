package ch.trancee.meshlink.messaging

import ch.trancee.meshlink.api.PeerIdHex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class SlidingWindowRateLimiterTest {
    @Test
    public fun tryAcquire_allowsUpToTheConfiguredBurstPerPeerPair(): Unit {
        // Arrange
        val rateLimiter = SlidingWindowRateLimiter(windowMillis = 100L, maxMessagesPerWindow = 2)
        val peerPair = PeerPair(
            senderPeerId = PeerIdHex(value = "00112233"),
            recipientPeerId = PeerIdHex(value = "44556677"),
        )

        // Act
        val first = rateLimiter.tryAcquire(peerPair = peerPair, nowEpochMillis = 0L)
        val second = rateLimiter.tryAcquire(peerPair = peerPair, nowEpochMillis = 1L)
        val third = rateLimiter.tryAcquire(peerPair = peerPair, nowEpochMillis = 2L)

        // Assert
        assertEquals(expected = true, actual = first)
        assertEquals(expected = true, actual = second)
        assertEquals(expected = false, actual = third)
        assertEquals(expected = 2, actual = rateLimiter.inFlightCount(peerPair = peerPair))
    }

    @Test
    public fun tryAcquire_expiresOldEntriesAfterTheWindowPasses(): Unit {
        // Arrange
        val rateLimiter = SlidingWindowRateLimiter(windowMillis = 100L, maxMessagesPerWindow = 1)
        val peerPair = PeerPair(
            senderPeerId = PeerIdHex(value = "00112233"),
            recipientPeerId = PeerIdHex(value = "44556677"),
        )
        rateLimiter.tryAcquire(peerPair = peerPair, nowEpochMillis = 0L)

        // Act
        val actual = rateLimiter.tryAcquire(peerPair = peerPair, nowEpochMillis = 100L)

        // Assert
        assertEquals(expected = true, actual = actual)
        assertEquals(expected = 1, actual = rateLimiter.inFlightCount(peerPair = peerPair))
    }

    @Test
    public fun tryAcquire_tracksPeerPairsIndependently(): Unit {
        // Arrange
        val rateLimiter = SlidingWindowRateLimiter(windowMillis = 100L, maxMessagesPerWindow = 1)
        val firstPeerPair = PeerPair(
            senderPeerId = PeerIdHex(value = "00112233"),
            recipientPeerId = PeerIdHex(value = "44556677"),
        )
        val secondPeerPair = PeerPair(
            senderPeerId = PeerIdHex(value = "00112233"),
            recipientPeerId = PeerIdHex(value = "8899aabb"),
        )
        rateLimiter.tryAcquire(peerPair = firstPeerPair, nowEpochMillis = 0L)

        // Act
        val actual = rateLimiter.tryAcquire(peerPair = secondPeerPair, nowEpochMillis = 1L)

        // Assert
        assertEquals(expected = true, actual = actual)
        assertEquals(expected = 1, actual = rateLimiter.inFlightCount(peerPair = firstPeerPair))
        assertEquals(expected = 1, actual = rateLimiter.inFlightCount(peerPair = secondPeerPair))
    }

    @Test
    public fun inFlightCount_returnsZeroWhenAPeerPairHasNotBeenSeenYet(): Unit {
        // Arrange
        val rateLimiter = SlidingWindowRateLimiter(windowMillis = 100L, maxMessagesPerWindow = 1)
        val peerPair = PeerPair(
            senderPeerId = PeerIdHex(value = "00112233"),
            recipientPeerId = PeerIdHex(value = "44556677"),
        )

        // Act
        val actual = rateLimiter.inFlightCount(peerPair = peerPair)

        // Assert
        assertEquals(expected = 0, actual = actual)
    }

    @Test
    public fun invalidInputs_areRejected(): Unit {
        // Arrange
        val peerPair = PeerPair(
            senderPeerId = PeerIdHex(value = "00112233"),
            recipientPeerId = PeerIdHex(value = "44556677"),
        )

        // Act
        val windowError = assertFailsWith<IllegalArgumentException> {
            SlidingWindowRateLimiter(windowMillis = 0L, maxMessagesPerWindow = 1)
        }
        val limitError = assertFailsWith<IllegalArgumentException> {
            SlidingWindowRateLimiter(windowMillis = 1L, maxMessagesPerWindow = 0)
        }
        val rateLimiter = SlidingWindowRateLimiter(windowMillis = 1L, maxMessagesPerWindow = 1)
        val timestampError = assertFailsWith<IllegalArgumentException> {
            rateLimiter.tryAcquire(peerPair = peerPair, nowEpochMillis = -1L)
        }

        // Assert
        assertEquals(expected = "SlidingWindowRateLimiter windowMillis must be greater than 0.", actual = windowError.message)
        assertEquals(expected = "SlidingWindowRateLimiter maxMessagesPerWindow must be greater than 0.", actual = limitError.message)
        assertEquals(expected = "SlidingWindowRateLimiter nowEpochMillis must be greater than or equal to 0.", actual = timestampError.message)
    }
}
