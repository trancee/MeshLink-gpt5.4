package ch.trancee.meshlink.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class PseudonymVerificationTest {
    @Test
    public fun isValidForCurrentWindow_acceptsTheCurrentEpochPseudonym(): Unit {
        // Arrange
        val rotator = PseudonymRotator(
            cryptoProvider = FakeCryptoProvider(),
            epochDurationMillis = 100L,
        )
        val identityKey = byteArrayOf(0x11, 0x22)
        val candidate = rotator.pseudonymForEpoch(identityKey = identityKey, epoch = 2L)

        // Act
        val actual = rotator.isValidForCurrentWindow(
            candidate = candidate,
            identityKey = identityKey,
            timestampMillis = 250L,
        )

        // Assert
        assertEquals(expected = true, actual = actual)
    }

    @Test
    public fun isValidForCurrentWindow_acceptsPreviousAndNextEpochPseudonyms(): Unit {
        // Arrange
        val rotator = PseudonymRotator(
            cryptoProvider = FakeCryptoProvider(),
            epochDurationMillis = 100L,
        )
        val identityKey = byteArrayOf(0x11, 0x22)
        val previous = rotator.pseudonymForEpoch(identityKey = identityKey, epoch = 1L)
        val next = rotator.pseudonymForEpoch(identityKey = identityKey, epoch = 3L)

        // Act
        val previousActual = rotator.isValidForCurrentWindow(
            candidate = previous,
            identityKey = identityKey,
            timestampMillis = 250L,
        )
        val nextActual = rotator.isValidForCurrentWindow(
            candidate = next,
            identityKey = identityKey,
            timestampMillis = 250L,
        )

        // Assert
        assertEquals(expected = true, actual = previousActual)
        assertEquals(expected = true, actual = nextActual)
    }

    @Test
    public fun isValidForCurrentWindow_rejectsPseudonymsOutsideTheToleranceWindow(): Unit {
        // Arrange
        val rotator = PseudonymRotator(
            cryptoProvider = FakeCryptoProvider(),
            epochDurationMillis = 100L,
        )
        val identityKey = byteArrayOf(0x11, 0x22)
        val candidate = rotator.pseudonymForEpoch(identityKey = identityKey, epoch = 4L)

        // Act
        val actual = rotator.isValidForCurrentWindow(
            candidate = candidate,
            identityKey = identityKey,
            timestampMillis = 250L,
        )

        // Assert
        assertEquals(expected = false, actual = actual)
    }

    @Test
    public fun isValidForCurrentWindow_handlesEpochZeroWithoutUnderflow(): Unit {
        // Arrange
        val rotator = PseudonymRotator(
            cryptoProvider = FakeCryptoProvider(),
            epochDurationMillis = 100L,
        )
        val identityKey = byteArrayOf(0x11, 0x22)
        val previousEpochCandidate = rotator.pseudonymForEpoch(identityKey = identityKey, epoch = 1L)

        // Act
        val actual = rotator.isValidForCurrentWindow(
            candidate = previousEpochCandidate,
            identityKey = identityKey,
            timestampMillis = 0L,
        )

        // Assert
        assertEquals(expected = true, actual = actual)
    }

    @Test
    public fun isValidForCurrentWindow_rejectsCandidatesWithUnexpectedLengths(): Unit {
        // Arrange
        val rotator = PseudonymRotator(cryptoProvider = FakeCryptoProvider())

        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            rotator.isValidForCurrentWindow(
                candidate = byteArrayOf(0x01),
                identityKey = byteArrayOf(0x11),
                timestampMillis = 0L,
            )
        }

        // Assert
        assertEquals(
            expected = "PseudonymRotator candidate pseudonym must be exactly 12 bytes.",
            actual = error.message,
        )
    }
}
