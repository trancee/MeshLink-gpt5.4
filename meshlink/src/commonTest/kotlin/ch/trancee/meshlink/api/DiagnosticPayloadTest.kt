package ch.trancee.meshlink.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

public class DiagnosticPayloadTest {
    @Test
    public fun none_representsPayloadFreeDiagnostics(): Unit {
        // Arrange
        val payload: DiagnosticPayload = DiagnosticPayload.None

        // Act / Assert
        assertEquals(expected = DiagnosticPayload.None, actual = payload)
    }

    @Test
    public fun handshakeFailure_retainsPeerAndReason(): Unit {
        // Arrange
        val expectedPeerId = PeerIdHex(value = "0a0b")
        val expectedReason = "trust mismatch"

        // Act
        val payload: DiagnosticPayload = DiagnosticPayload.HandshakeFailure(
            peerId = expectedPeerId,
            reason = expectedReason,
        )

        // Assert
        val actual: DiagnosticPayload.HandshakeFailure = assertIs<DiagnosticPayload.HandshakeFailure>(payload)
        assertEquals(expected = expectedPeerId, actual = actual.peerId)
        assertEquals(expected = expectedReason, actual = actual.reason)
    }

    @Test
    public fun transferProgress_retainsProgressValues(): Unit {
        // Arrange
        val expectedTransferId = "transfer-1"

        // Act
        val payload: DiagnosticPayload = DiagnosticPayload.TransferProgress(
            transferId = expectedTransferId,
            bytesTransferred = 64,
            totalBytes = 128,
        )

        // Assert
        val actual: DiagnosticPayload.TransferProgress = assertIs<DiagnosticPayload.TransferProgress>(payload)
        assertEquals(expected = expectedTransferId, actual = actual.transferId)
        assertEquals(expected = 64L, actual = actual.bytesTransferred)
        assertEquals(expected = 128L, actual = actual.totalBytes)
    }

    @Test
    public fun peerLifecycle_retainsPeerAndState(): Unit {
        // Arrange
        val expectedPeerId = PeerIdHex(value = "0c0d")
        val expectedState: PeerState = PeerState.Connected

        // Act
        val payload: DiagnosticPayload = DiagnosticPayload.PeerLifecycle(
            peerId = expectedPeerId,
            state = expectedState,
        )

        // Assert
        val actual: DiagnosticPayload.PeerLifecycle = assertIs<DiagnosticPayload.PeerLifecycle>(payload)
        assertEquals(expected = expectedPeerId, actual = actual.peerId)
        assertEquals(expected = expectedState, actual = actual.state)
    }

    @Test
    public fun routingChange_retainsDestinationAndMetric(): Unit {
        // Arrange
        val expectedPeerId = PeerIdHex(value = "0e0f")

        // Act
        val payload: DiagnosticPayload = DiagnosticPayload.RoutingChange(
            destinationPeerId = expectedPeerId,
            metric = 42,
        )

        // Assert
        val actual: DiagnosticPayload.RoutingChange = assertIs<DiagnosticPayload.RoutingChange>(payload)
        assertEquals(expected = expectedPeerId, actual = actual.destinationPeerId)
        assertEquals(expected = 42, actual = actual.metric)
    }

    @Test
    public fun bufferPressure_retainsUsageAndDropCounts(): Unit {
        // Arrange
        // Act
        val payload: DiagnosticPayload = DiagnosticPayload.BufferPressure(
            usedBytes = 512,
            droppedEvents = 3,
        )

        // Assert
        val actual: DiagnosticPayload.BufferPressure = assertIs<DiagnosticPayload.BufferPressure>(payload)
        assertEquals(expected = 512, actual = actual.usedBytes)
        assertEquals(expected = 3, actual = actual.droppedEvents)
    }

    @Test
    public fun powerTierChanged_retainsPreviousAndCurrentTiers(): Unit {
        // Arrange
        // Act
        val payload: DiagnosticPayload = DiagnosticPayload.PowerTierChanged(
            previousTier = "BALANCED",
            currentTier = "LOW",
        )

        // Assert
        val actual: DiagnosticPayload.PowerTierChanged = assertIs<DiagnosticPayload.PowerTierChanged>(payload)
        assertEquals(expected = "BALANCED", actual = actual.previousTier)
        assertEquals(expected = "LOW", actual = actual.currentTier)
    }

    @Test
    public fun internalError_retainsMessage(): Unit {
        // Arrange
        val expectedMessage = "unexpected state"

        // Act
        val payload: DiagnosticPayload = DiagnosticPayload.InternalError(message = expectedMessage)

        // Assert
        val actual: DiagnosticPayload.InternalError = assertIs<DiagnosticPayload.InternalError>(payload)
        assertEquals(expected = expectedMessage, actual = actual.message)
    }
}
