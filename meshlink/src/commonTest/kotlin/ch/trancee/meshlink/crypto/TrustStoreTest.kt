package ch.trancee.meshlink.crypto

import ch.trancee.meshlink.api.TrustMode
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

public class TrustStoreTest {
    @Test
    public fun evaluate_pinsNewPeerInTofuMode(): Unit {
        // Arrange
        val store = TrustStore()
        val peerId: ByteArray = byteArrayOf(0x01, 0x02)
        val publicKey: ByteArray = byteArrayOf(0x11, 0x12, 0x13)

        // Act
        val decision: TrustDecision = store.evaluate(
            peerId = peerId,
            presentedPublicKey = publicKey,
            mode = TrustMode.TOFU,
        )
        val actualPinnedKey: ByteArray? = store.pinnedKey(peerId = peerId)

        // Assert
        assertEquals(
            expected = TrustDecision.Pinned,
            actual = decision,
            message = "TrustStore should pin first-contact peers in TOFU mode",
        )
        assertContentEquals(
            expected = publicKey,
            actual = actualPinnedKey,
            message = "TrustStore should persist the pinned key for TOFU peers",
        )
    }

    @Test
    public fun evaluate_acceptsPinnedPeerWhenKeyMatches(): Unit {
        // Arrange
        val store = TrustStore()
        val peerId: ByteArray = byteArrayOf(0x03, 0x04)
        val publicKey: ByteArray = byteArrayOf(0x21, 0x22, 0x23)
        store.pin(peerId = peerId, publicKey = publicKey)

        // Act
        val decision: TrustDecision = store.evaluate(
            peerId = peerId,
            presentedPublicKey = publicKey,
            mode = TrustMode.STRICT,
        )

        // Assert
        assertEquals(
            expected = TrustDecision.Accepted,
            actual = decision,
            message = "TrustStore should accept peers whose presented key matches the pinned key",
        )
    }

    @Test
    public fun evaluate_rejectsUnknownPeerInStrictMode(): Unit {
        // Arrange
        val store = TrustStore()

        // Act
        val decision: TrustDecision = store.evaluate(
            peerId = byteArrayOf(0x05, 0x06),
            presentedPublicKey = byteArrayOf(0x31, 0x32, 0x33),
            mode = TrustMode.STRICT,
        )

        // Assert
        val rejected: TrustDecision.Rejected = assertIs<TrustDecision.Rejected>(
            value = decision,
            message = "TrustStore should reject unknown peers in STRICT mode",
        )
        assertEquals(
            expected = "Peer is not pinned in STRICT mode.",
            actual = rejected.reason,
            message = "TrustStore should explain why unknown peers are rejected in STRICT mode",
        )
    }

    @Test
    public fun evaluate_rejectsMismatchedPinnedKeyInStrictMode(): Unit {
        // Arrange
        val store = TrustStore()
        val peerId: ByteArray = byteArrayOf(0x07, 0x08)
        store.pin(peerId = peerId, publicKey = byteArrayOf(0x41, 0x42, 0x43))

        // Act
        val decision: TrustDecision = store.evaluate(
            peerId = peerId,
            presentedPublicKey = byteArrayOf(0x51, 0x52, 0x53),
            mode = TrustMode.STRICT,
        )

        // Assert
        val rejected: TrustDecision.Rejected = assertIs<TrustDecision.Rejected>(
            value = decision,
            message = "TrustStore should reject mismatched keys in STRICT mode",
        )
        assertEquals(
            expected = "Presented key does not match pinned key.",
            actual = rejected.reason,
            message = "TrustStore should explain why mismatched keys are rejected",
        )
    }

    @Test
    public fun evaluate_rejectsMismatchedPinnedKeyInTofuModeAfterInitialPin(): Unit {
        // Arrange
        val store = TrustStore()
        val peerId: ByteArray = byteArrayOf(0x08, 0x09)
        store.pin(peerId = peerId, publicKey = byteArrayOf(0x11, 0x12, 0x13))

        // Act
        val decision: TrustDecision = store.evaluate(
            peerId = peerId,
            presentedPublicKey = byteArrayOf(0x21, 0x22, 0x23),
            mode = TrustMode.TOFU,
        )

        // Assert
        val rejected: TrustDecision.Rejected = assertIs<TrustDecision.Rejected>(
            value = decision,
            message = "TrustStore should reject mismatched keys in TOFU mode after the first contact has been pinned",
        )
        assertEquals(
            expected = "Presented key does not match pinned key.",
            actual = rejected.reason,
            message = "TrustStore should explain why TOFU rejects mismatched pinned keys",
        )
    }

    @Test
    public fun evaluate_requestsPromptForUnknownPeerInPromptMode(): Unit {
        // Arrange
        val store = TrustStore()

        // Act
        val decision: TrustDecision = store.evaluate(
            peerId = byteArrayOf(0x09, 0x0A),
            presentedPublicKey = byteArrayOf(0x61, 0x62, 0x63),
            mode = TrustMode.PROMPT,
        )

        // Assert
        val promptRequired: TrustDecision.PromptRequired = assertIs<TrustDecision.PromptRequired>(
            value = decision,
            message = "TrustStore should request a prompt for unknown peers in PROMPT mode",
        )
        assertNull(
            actual = promptRequired.existingPublicKey,
            message = "TrustStore should not report an existing pinned key when the peer is unknown",
        )
    }

    @Test
    public fun evaluate_requestsPromptForMismatchedPinnedKeyInPromptMode(): Unit {
        // Arrange
        val store = TrustStore()
        val peerId: ByteArray = byteArrayOf(0x0B, 0x0C)
        val existingKey: ByteArray = byteArrayOf(0x71, 0x72, 0x73)
        store.pin(peerId = peerId, publicKey = existingKey)

        // Act
        val decision: TrustDecision = store.evaluate(
            peerId = peerId,
            presentedPublicKey = byteArrayOf(0x74, 0x75, 0x76),
            mode = TrustMode.PROMPT,
        )

        // Assert
        val promptRequired: TrustDecision.PromptRequired = assertIs<TrustDecision.PromptRequired>(
            value = decision,
            message = "TrustStore should request a prompt when a pinned peer presents a different key in PROMPT mode",
        )
        assertContentEquals(
            expected = existingKey,
            actual = promptRequired.existingPublicKey,
            message = "TrustStore should expose the existing pinned key to the prompt flow",
        )
    }

    @Test
    public fun pinnedKey_returnsNullForUnknownPeer(): Unit {
        // Arrange
        val store = TrustStore()
        val peerId: ByteArray = byteArrayOf(0x0C, 0x0D)

        // Act
        val actual: ByteArray? = store.pinnedKey(peerId = peerId)

        // Assert
        assertNull(
            actual = actual,
            message = "TrustStore should return null when no key has been pinned for the peer",
        )
    }

    @Test
    public fun pinnedKey_returnsDefensiveCopy(): Unit {
        // Arrange
        val store = TrustStore()
        val peerId: ByteArray = byteArrayOf(0x0D, 0x0E)
        store.pin(peerId = peerId, publicKey = byteArrayOf(0x01, 0x02, 0x03))
        val firstRead: ByteArray? = store.pinnedKey(peerId = peerId)
        firstRead!![0] = 0x7F

        // Act
        val secondRead: ByteArray? = store.pinnedKey(peerId = peerId)

        // Assert
        assertContentEquals(
            expected = byteArrayOf(0x01, 0x02, 0x03),
            actual = secondRead,
            message = "TrustStore should protect pinned keys from caller mutation by returning defensive copies",
        )
    }
}
