package ch.trancee.meshlink.engine

import ch.trancee.meshlink.api.PeerIdHex
import ch.trancee.meshlink.transport.VirtualMeshTransport
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse

public class PseudonymRotationIntegrationTest {
    @Test
    public fun engine_rotatesPseudonymsAcrossEpochsAndVerifiesWithinTolerance(): Unit {
        // Arrange
        val engine = MeshEngine.create(
            config = MeshEngineConfig.default(),
            transport = VirtualMeshTransport(localPeerId = PeerIdHex(value = "00112233")),
            cryptoProvider = FakeCryptoProvider(),
        )
        val identityKey = byteArrayOf(0x11, 0x22)
        val firstTimestamp = 0L
        val secondTimestamp = PseudonymRotator.DEFAULT_EPOCH_DURATION_MILLIS

        // Act
        val first = engine.pseudonymAt(identityKey = identityKey, timestampMillis = firstTimestamp)
        val second = engine.pseudonymAt(identityKey = identityKey, timestampMillis = secondTimestamp)
        val previousEpochStillValid = engine.verifyPseudonym(
            candidate = first,
            identityKey = identityKey,
            timestampMillis = secondTimestamp,
        )
        val staleCandidate = engine.pseudonymRotator.pseudonymForEpoch(identityKey = identityKey, epoch = 5L)
        val staleCandidateValid = engine.verifyPseudonym(
            candidate = staleCandidate,
            identityKey = identityKey,
            timestampMillis = 10 * PseudonymRotator.DEFAULT_EPOCH_DURATION_MILLIS,
        )

        // Assert
        assertFalse(actual = first.contentEquals(second))
        assertEquals(expected = true, actual = previousEpochStillValid)
        assertEquals(expected = false, actual = staleCandidateValid)
    }

    @Test
    public fun engine_identityApi_matchesTheUnderlyingRotatorOutput(): Unit {
        // Arrange
        val engine = MeshEngine.create(
            config = MeshEngineConfig.default(),
            transport = VirtualMeshTransport(localPeerId = PeerIdHex(value = "00112233")),
            cryptoProvider = FakeCryptoProvider(),
        )
        val identityKey = byteArrayOf(0x33, 0x44)

        // Act
        val expected = engine.pseudonymRotator.pseudonymAt(
            identityKey = identityKey,
            timestampMillis = 1234L,
        )
        val actual = engine.pseudonymAt(
            identityKey = identityKey,
            timestampMillis = 1234L,
        )

        // Assert
        assertContentEquals(expected = expected, actual = actual)
    }
}
