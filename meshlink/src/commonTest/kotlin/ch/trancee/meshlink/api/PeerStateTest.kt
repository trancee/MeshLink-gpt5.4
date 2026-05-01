package ch.trancee.meshlink.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

public class PeerStateTest {
    @Test
    public fun singletonStates_areAccessibleThroughTheSealedHierarchy(): Unit {
        // Arrange
        val discovered: PeerState = PeerState.Discovered
        val connecting: PeerState = PeerState.Connecting
        val connected: PeerState = PeerState.Connected
        val disconnected: PeerState = PeerState.Disconnected

        // Act / Assert
        assertEquals(expected = PeerState.Discovered, actual = discovered)
        assertEquals(expected = PeerState.Connecting, actual = connecting)
        assertEquals(expected = PeerState.Connected, actual = connected)
        assertEquals(expected = PeerState.Disconnected, actual = disconnected)
    }

    @Test
    public fun failed_retainsFailureReason(): Unit {
        // Arrange
        val expectedReason: String = "timeout"

        // Act
        val actual: PeerState = PeerState.Failed(reason = expectedReason)

        // Assert
        val failed: PeerState.Failed = assertIs<PeerState.Failed>(actual)
        assertEquals(
            expected = expectedReason,
            actual = failed.reason,
            message = "PeerState.Failed should retain the failure reason",
        )
    }
}
