package ch.trancee.meshlink.power

import kotlin.test.Test
import kotlin.test.assertEquals

public class GracefulDrainIntegrationTest {
    @Test
    public fun evaluate_keepsInFlightConnectionsAliveUntilTransfersFinish(): Unit {
        // Arrange
        val manager = GracefulDrainManager()
        val inFlightConnection = ManagedConnection(
            peerKey = PeerKey(value = "in-flight"),
            transferStatus = TransferStatus.IN_FLIGHT,
            lastActivityEpochMillis = 1L,
        )
        val idleConnection = ManagedConnection(
            peerKey = PeerKey(value = "idle"),
            transferStatus = TransferStatus.IDLE,
            lastActivityEpochMillis = 2L,
        )
        val completeConnection = ManagedConnection(
            peerKey = PeerKey(value = "complete"),
            transferStatus = TransferStatus.COMPLETE,
            lastActivityEpochMillis = 3L,
        )

        // Act
        val actual = manager.evaluate(connections = listOf(inFlightConnection, idleConnection, completeConnection))

        // Assert
        assertEquals(expected = false, actual = actual.drainComplete)
        assertEquals(expected = listOf(idleConnection.peerKey, completeConnection.peerKey), actual = actual.connectionsToClose)
    }

    @Test
    public fun evaluate_closesAllConnectionsOnceNoTransfersRemain(): Unit {
        // Arrange
        val manager = GracefulDrainManager()
        val idleConnection = ManagedConnection(
            peerKey = PeerKey(value = "idle"),
            transferStatus = TransferStatus.IDLE,
            lastActivityEpochMillis = 1L,
        )
        val completeConnection = ManagedConnection(
            peerKey = PeerKey(value = "complete"),
            transferStatus = TransferStatus.COMPLETE,
            lastActivityEpochMillis = 2L,
        )

        // Act
        val actual = manager.evaluate(connections = listOf(idleConnection, completeConnection))

        // Assert
        assertEquals(expected = true, actual = actual.drainComplete)
        assertEquals(expected = listOf(idleConnection.peerKey, completeConnection.peerKey), actual = actual.connectionsToClose)
    }

    @Test
    public fun evaluate_handlesAnEmptyConnectionSet(): Unit {
        // Arrange
        val manager = GracefulDrainManager()

        // Act
        val actual = manager.evaluate(connections = emptyList())

        // Assert
        assertEquals(expected = true, actual = actual.drainComplete)
        assertEquals(expected = emptyList(), actual = actual.connectionsToClose)
    }
}
