package ch.trancee.meshlink.crypto.noise

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

public class DhCacheKeyTest {
    @Test
    public fun equals_returnsTrueForIdenticalKeyMaterial(): Unit {
        // Arrange
        val left = DhCacheKey(privateKey = byteArrayOf(0x01), publicKey = byteArrayOf(0x02))
        val right = DhCacheKey(privateKey = byteArrayOf(0x01), publicKey = byteArrayOf(0x02))

        // Act
        val actual: Boolean = left == right

        // Assert
        assertTrue(
            actual = actual,
            message = "DhCacheKey should compare identical key material by content",
        )
        assertEquals(
            expected = left.hashCode(),
            actual = right.hashCode(),
            message = "DhCacheKey should keep hashCode aligned with content-based equality",
        )
    }

    @Test
    public fun equals_returnsFalseForDifferentKeyMaterial(): Unit {
        // Arrange
        val left = DhCacheKey(privateKey = byteArrayOf(0x01), publicKey = byteArrayOf(0x02))
        val right = DhCacheKey(privateKey = byteArrayOf(0x01), publicKey = byteArrayOf(0x03))

        // Act
        val actual: Boolean = left == right

        // Assert
        assertFalse(
            actual = actual,
            message = "DhCacheKey should reject different key material",
        )
    }

    @Test
    public fun equals_returnsFalseForDifferentPrivateKeyMaterial(): Unit {
        // Arrange
        val left = DhCacheKey(privateKey = byteArrayOf(0x01), publicKey = byteArrayOf(0x02))
        val right = DhCacheKey(privateKey = byteArrayOf(0x03), publicKey = byteArrayOf(0x02))

        // Act
        val actual: Boolean = left == right

        // Assert
        assertFalse(
            actual = actual,
            message = "DhCacheKey should reject different private key material",
        )
    }

    @Test
    public fun equals_returnsFalseForObjectsOfDifferentType(): Unit {
        // Arrange
        val key = DhCacheKey(privateKey = byteArrayOf(0x01), publicKey = byteArrayOf(0x02))

        // Act
        val actual: Boolean = key.equals(other = "not-a-cache-key")

        // Assert
        assertFalse(
            actual = actual,
            message = "DhCacheKey should reject objects of unrelated types",
        )
    }
}
