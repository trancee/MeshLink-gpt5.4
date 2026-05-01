package ch.trancee.meshlink.engine

import ch.trancee.meshlink.crypto.CryptoProvider
import ch.trancee.meshlink.crypto.KeyPair
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

public class PseudonymRotatorTest {
    @Test
    public fun epochFor_changesAtConfiguredBoundaries(): Unit {
        // Arrange
        val rotator = PseudonymRotator(
            cryptoProvider = FakeCryptoProvider(),
            epochDurationMillis = 100L,
        )

        // Act
        val firstEpoch = rotator.epochFor(timestampMillis = 0L)
        val boundaryEpoch = rotator.epochFor(timestampMillis = 100L)
        val laterEpoch = rotator.epochFor(timestampMillis = 250L)

        // Assert
        assertEquals(expected = 0L, actual = firstEpoch)
        assertEquals(expected = 1L, actual = boundaryEpoch)
        assertEquals(expected = 2L, actual = laterEpoch)
    }

    @Test
    public fun staggerMillis_isDeterministicAndBoundedPerNode(): Unit {
        // Arrange
        val rotator = PseudonymRotator(
            cryptoProvider = FakeCryptoProvider(),
            maxStaggerMillis = 10L,
        )
        val nodeId = byteArrayOf(0x01, 0x02, 0x03)

        // Act
        val first = rotator.staggerMillis(nodeId = nodeId)
        val second = rotator.staggerMillis(nodeId = nodeId)

        // Assert
        assertEquals(expected = first, actual = second)
        assertEquals(expected = true, actual = first in 0L..10L)
    }

    @Test
    public fun pseudonymAt_rotatesAcrossEpochBoundaries(): Unit {
        // Arrange
        val rotator = PseudonymRotator(
            cryptoProvider = FakeCryptoProvider(),
            epochDurationMillis = 100L,
        )
        val identityKey = byteArrayOf(0x11, 0x22, 0x33)

        // Act
        val first = rotator.pseudonymAt(identityKey = identityKey, timestampMillis = 50L)
        val second = rotator.pseudonymAt(identityKey = identityKey, timestampMillis = 150L)

        // Assert
        assertNotEquals(illegal = first.toList(), actual = second.toList())
    }

    @Test
    public fun pseudonymForEpoch_returnsTheConfiguredAdvertisementLength(): Unit {
        // Arrange
        val rotator = PseudonymRotator(
            cryptoProvider = FakeCryptoProvider(),
            pseudonymLengthBytes = 12,
        )

        // Act
        val actual = rotator.pseudonymForEpoch(
            identityKey = byteArrayOf(0x01),
            epoch = 1L,
        )

        // Assert
        assertEquals(expected = 12, actual = actual.size)
    }

    @Test
    public fun init_rejectsInvalidEpochDurations(): Unit {
        // Arrange
        val expectedMessage = "PseudonymRotator epochDurationMillis must be greater than 0."

        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            PseudonymRotator(
                cryptoProvider = FakeCryptoProvider(),
                epochDurationMillis = 0L,
            )
        }

        // Assert
        assertEquals(expected = expectedMessage, actual = error.message)
    }

    @Test
    public fun init_rejectsInvalidPseudonymLengths(): Unit {
        // Arrange
        val expectedMessage = "PseudonymRotator pseudonymLengthBytes must be between 1 and 32."

        // Act
        val tooSmallError = assertFailsWith<IllegalArgumentException> {
            PseudonymRotator(
                cryptoProvider = FakeCryptoProvider(),
                pseudonymLengthBytes = 0,
            )
        }
        val tooLargeError = assertFailsWith<IllegalArgumentException> {
            PseudonymRotator(
                cryptoProvider = FakeCryptoProvider(),
                pseudonymLengthBytes = 33,
            )
        }

        // Assert
        assertEquals(expected = expectedMessage, actual = tooSmallError.message)
        assertEquals(expected = expectedMessage, actual = tooLargeError.message)
    }

    @Test
    public fun init_rejectsNegativeMaxStaggerValues(): Unit {
        // Arrange
        val expectedMessage = "PseudonymRotator maxStaggerMillis must be greater than or equal to 0."

        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            PseudonymRotator(
                cryptoProvider = FakeCryptoProvider(),
                maxStaggerMillis = -1L,
            )
        }

        // Assert
        assertEquals(expected = expectedMessage, actual = error.message)
    }

    @Test
    public fun epochFor_rejectsNegativeTimestamps(): Unit {
        // Arrange
        val rotator = PseudonymRotator(cryptoProvider = FakeCryptoProvider())

        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            rotator.epochFor(timestampMillis = -1L)
        }

        // Assert
        assertEquals(
            expected = "PseudonymRotator timestampMillis must be greater than or equal to 0.",
            actual = error.message,
        )
    }

    @Test
    public fun staggerMillis_rejectsEmptyNodeIds(): Unit {
        // Arrange
        val rotator = PseudonymRotator(cryptoProvider = FakeCryptoProvider())

        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            rotator.staggerMillis(nodeId = byteArrayOf())
        }

        // Assert
        assertEquals(
            expected = "PseudonymRotator nodeId must not be empty.",
            actual = error.message,
        )
    }

    @Test
    public fun staggerMillis_returnsZeroWhenNoStaggerIsConfigured(): Unit {
        // Arrange
        val rotator = PseudonymRotator(
            cryptoProvider = FakeCryptoProvider(),
            maxStaggerMillis = 0L,
        )

        // Act
        val actual = rotator.staggerMillis(nodeId = byteArrayOf(0x01))

        // Assert
        assertEquals(expected = 0L, actual = actual)
    }

    @Test
    public fun pseudonymForEpoch_rejectsInvalidInputs(): Unit {
        // Arrange
        val rotator = PseudonymRotator(cryptoProvider = FakeCryptoProvider())

        // Act
        val emptyKeyError = assertFailsWith<IllegalArgumentException> {
            rotator.pseudonymForEpoch(identityKey = byteArrayOf(), epoch = 0L)
        }
        val negativeEpochError = assertFailsWith<IllegalArgumentException> {
            rotator.pseudonymForEpoch(identityKey = byteArrayOf(0x01), epoch = -1L)
        }

        // Assert
        assertEquals(expected = "PseudonymRotator identityKey must not be empty.", actual = emptyKeyError.message)
        assertEquals(expected = "PseudonymRotator epoch must be greater than or equal to 0.", actual = negativeEpochError.message)
    }

    @Test
    public fun pseudonymAt_delegatesToEpochBasedDerivation(): Unit {
        // Arrange
        val rotator = PseudonymRotator(
            cryptoProvider = FakeCryptoProvider(),
            epochDurationMillis = 100L,
        )
        val identityKey = byteArrayOf(0x55)
        val expected = rotator.pseudonymForEpoch(identityKey = identityKey, epoch = 2L)

        // Act
        val actual = rotator.pseudonymAt(identityKey = identityKey, timestampMillis = 250L)

        // Assert
        assertContentEquals(expected = expected, actual = actual)
    }
}

internal class FakeCryptoProvider : CryptoProvider {
    override fun generateX25519KeyPair(): KeyPair = error("Unused in test")

    override fun generateEd25519KeyPair(): KeyPair = error("Unused in test")

    override fun x25519(privateKey: ByteArray, publicKey: ByteArray): ByteArray = error("Unused in test")

    override fun ed25519Sign(privateKey: ByteArray, message: ByteArray): ByteArray = error("Unused in test")

    override fun ed25519Verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean = error("Unused in test")

    override fun chaCha20Poly1305Encrypt(key: ByteArray, nonce: ByteArray, aad: ByteArray, plaintext: ByteArray): ByteArray = error("Unused in test")

    override fun chaCha20Poly1305Decrypt(key: ByteArray, nonce: ByteArray, aad: ByteArray, ciphertext: ByteArray): ByteArray = error("Unused in test")

    override fun hkdfSha256(ikm: ByteArray, salt: ByteArray, info: ByteArray, outputLength: Int): ByteArray = error("Unused in test")

    override fun hmacSha256(key: ByteArray, message: ByteArray): ByteArray {
        return ByteArray(size = 32) { index ->
            val keyByte: Int = key[index % key.size].toInt() and 0xFF
            val messageByte: Int = message[index % message.size].toInt() and 0xFF
            ((keyByte + messageByte + index) and 0xFF).toByte()
        }
    }
}
