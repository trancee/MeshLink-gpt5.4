package ch.trancee.meshlink.engine

import ch.trancee.meshlink.api.PeerIdHex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class MeshStateManagerTest {
    @Test
    public fun defaultTimeout_matchesTheSweepContract(): Unit {
        // Arrange
        val expected = 30_000L

        // Act
        val actual = MeshStateManager.DEFAULT_STALE_PEER_TIMEOUT_MILLIS

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    public fun sweep_returnsOnlyPeersOlderThanTheTimeout(): Unit {
        // Arrange
        val manager = MeshStateManager(stalePeerTimeoutMillis = 100L)
        val stalePeerId = PeerIdHex(value = "00112233")
        val freshPeerId = PeerIdHex(value = "44556677")

        // Act
        val actual = manager.sweep(
            peers = listOf(
                ManagedPeer(peerId = stalePeerId, lastSeenEpochMillis = 0L),
                ManagedPeer(peerId = freshPeerId, lastSeenEpochMillis = 50L),
            ),
            routes = emptyList(),
            nowEpochMillis = 100L,
        )

        // Assert
        assertEquals(expected = listOf(stalePeerId), actual = actual.stalePeers)
        assertEquals(expected = emptyList(), actual = actual.expiredRoutes)
    }

    @Test
    public fun sweep_returnsRoutesThatHaveExpiredByNow(): Unit {
        // Arrange
        val manager = MeshStateManager(stalePeerTimeoutMillis = 100L)
        val expiredRoutePeerId = PeerIdHex(value = "00112233")
        val activeRoutePeerId = PeerIdHex(value = "44556677")

        // Act
        val actual = manager.sweep(
            peers = emptyList(),
            routes = listOf(
                ManagedRoute(destinationPeerId = expiredRoutePeerId, expiresAtEpochMillis = 100L),
                ManagedRoute(destinationPeerId = activeRoutePeerId, expiresAtEpochMillis = 101L),
            ),
            nowEpochMillis = 100L,
        )

        // Assert
        assertEquals(expected = emptyList(), actual = actual.stalePeers)
        assertEquals(expected = listOf(expiredRoutePeerId), actual = actual.expiredRoutes)
    }

    @Test
    public fun sweep_canReportBothStalePeersAndExpiredRoutesInTheSamePass(): Unit {
        // Arrange
        val manager = MeshStateManager(stalePeerTimeoutMillis = 100L)
        val stalePeerId = PeerIdHex(value = "00112233")
        val expiredRoutePeerId = PeerIdHex(value = "44556677")

        // Act
        val actual = manager.sweep(
            peers = listOf(ManagedPeer(peerId = stalePeerId, lastSeenEpochMillis = 0L)),
            routes = listOf(ManagedRoute(destinationPeerId = expiredRoutePeerId, expiresAtEpochMillis = 50L)),
            nowEpochMillis = 100L,
        )

        // Assert
        assertEquals(expected = listOf(stalePeerId), actual = actual.stalePeers)
        assertEquals(expected = listOf(expiredRoutePeerId), actual = actual.expiredRoutes)
    }

    @Test
    public fun sweep_rejectsNegativeTimestamps(): Unit {
        // Arrange
        val manager = MeshStateManager()

        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            manager.sweep(
                peers = emptyList(),
                routes = emptyList(),
                nowEpochMillis = -1L,
            )
        }

        // Assert
        assertEquals(
            expected = "MeshStateManager nowEpochMillis must be greater than or equal to 0.",
            actual = error.message,
        )
    }

    @Test
    public fun init_rejectsNonPositivePeerTimeouts(): Unit {
        // Arrange
        val expectedMessage = "MeshStateManager stalePeerTimeoutMillis must be greater than 0."

        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            MeshStateManager(stalePeerTimeoutMillis = 0L)
        }

        // Assert
        assertEquals(expected = expectedMessage, actual = error.message)
    }
}
