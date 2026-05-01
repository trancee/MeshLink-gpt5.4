package ch.trancee.meshlink.routing

import ch.trancee.meshlink.api.PeerIdHex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

public class PresenceTrackerTest {
  @Test
  public fun defaultTimeout_matchesThePresenceContract(): Unit {
    // Arrange
    val expected = 15_000L

    // Act
    val actual = PresenceTracker.DEFAULT_TIMEOUT_MILLIS

    // Assert
    assertEquals(expected = expected, actual = actual)
  }

  @Test
  public fun observe_emitsAppearedWhenAPeerIsSeenForTheFirstTime(): Unit {
    // Arrange
    val tracker = PresenceTracker(timeoutMillis = 100L)
    val peerId = PeerIdHex(value = "00112233")

    // Act
    val actual: PresenceEvent? = tracker.observe(peerId = peerId, nowEpochMillis = 1L)

    // Assert
    assertEquals(expected = PresenceEvent.Appeared(peerId = peerId), actual = actual)
    assertEquals(expected = setOf(peerId), actual = tracker.presentPeers())
  }

  @Test
  public fun observe_refreshesKnownPeersWithoutRepeatingTheAppearedEvent(): Unit {
    // Arrange
    val tracker = PresenceTracker(timeoutMillis = 100L)
    val peerId = PeerIdHex(value = "00112233")
    tracker.observe(peerId = peerId, nowEpochMillis = 1L)

    // Act
    val actual: PresenceEvent? = tracker.observe(peerId = peerId, nowEpochMillis = 50L)
    val sweepEvents: List<PresenceEvent> = tracker.sweep(nowEpochMillis = 149L)

    // Assert
    assertEquals(expected = null, actual = actual)
    assertTrue(actual = sweepEvents.isEmpty())
    assertEquals(expected = setOf(peerId), actual = tracker.presentPeers())
  }

  @Test
  public fun sweep_emitsDisappearedEventsForPeersThatTimedOut(): Unit {
    // Arrange
    val tracker = PresenceTracker(timeoutMillis = 100L)
    val stalePeer = PeerIdHex(value = "00112233")
    val freshPeer = PeerIdHex(value = "44556677")
    tracker.observe(peerId = stalePeer, nowEpochMillis = 0L)
    tracker.observe(peerId = freshPeer, nowEpochMillis = 50L)

    // Act
    val actual: List<PresenceEvent> = tracker.sweep(nowEpochMillis = 100L)

    // Assert
    assertEquals(expected = listOf(PresenceEvent.Disappeared(peerId = stalePeer)), actual = actual)
    assertEquals(expected = setOf(freshPeer), actual = tracker.presentPeers())
  }

  @Test
  public fun sweep_returnsAnEmptyListWhenNoPeersExpire(): Unit {
    // Arrange
    val tracker = PresenceTracker(timeoutMillis = 100L)
    tracker.observe(peerId = PeerIdHex(value = "00112233"), nowEpochMillis = 10L)

    // Act
    val actual: List<PresenceEvent> = tracker.sweep(nowEpochMillis = 50L)

    // Assert
    assertTrue(actual = actual.isEmpty())
  }

  @Test
  public fun observe_andSweep_rejectNegativeTimestamps(): Unit {
    // Arrange
    val tracker = PresenceTracker()
    val peerId = PeerIdHex(value = "00112233")

    // Act
    val observeError =
      assertFailsWith<IllegalArgumentException> {
        tracker.observe(peerId = peerId, nowEpochMillis = -1L)
      }
    val sweepError =
      assertFailsWith<IllegalArgumentException> { tracker.sweep(nowEpochMillis = -1L) }

    // Assert
    assertEquals(
      expected = "PresenceTracker nowEpochMillis must be greater than or equal to 0.",
      actual = observeError.message,
    )
    assertEquals(
      expected = "PresenceTracker nowEpochMillis must be greater than or equal to 0.",
      actual = sweepError.message,
    )
  }

  @Test
  public fun init_rejectsNonPositiveTimeouts(): Unit {
    // Arrange
    val expectedMessage = "PresenceTracker timeoutMillis must be greater than 0."

    // Act
    val error = assertFailsWith<IllegalArgumentException> { PresenceTracker(timeoutMillis = 0L) }

    // Assert
    assertEquals(expected = expectedMessage, actual = error.message)
  }
}
