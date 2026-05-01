package ch.trancee.meshlink.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals

public class CryptoTypesTest {
    @Test
    public fun keyPair_retainsPublicAndSecretKeyBytes(): Unit {
        // Arrange
        val expectedPublicKey: ByteArray = byteArrayOf(0x11, 0x12, 0x13)
        val expectedSecretKey: ByteArray = byteArrayOf(0x21, 0x22, 0x23)

        // Act
        val actual = KeyPair(
            publicKey = expectedPublicKey,
            secretKey = expectedSecretKey,
        )

        // Assert
        assertContentEquals(
            expected = expectedPublicKey,
            actual = actual.publicKey,
            message = "KeyPair should retain the public key bytes",
        )
        assertContentEquals(
            expected = expectedSecretKey,
            actual = actual.secretKey,
            message = "KeyPair should retain the secret key bytes",
        )
    }

    @Test
    public fun identity_retainsPublicKeySecretKeyAndKeyHash(): Unit {
        // Arrange
        val expectedPublicKey: ByteArray = byteArrayOf(0x31, 0x32, 0x33)
        val expectedSecretKey: ByteArray = byteArrayOf(0x41, 0x42, 0x43)
        val expectedKeyHash: ByteArray = byteArrayOf(0x51, 0x52, 0x53)

        // Act
        val actual = Identity(
            publicKey = expectedPublicKey,
            secretKey = expectedSecretKey,
            keyHash = expectedKeyHash,
        )

        // Assert
        assertContentEquals(
            expected = expectedPublicKey,
            actual = actual.publicKey,
            message = "Identity should retain the public key bytes",
        )
        assertContentEquals(
            expected = expectedSecretKey,
            actual = actual.secretKey,
            message = "Identity should retain the secret key bytes",
        )
        assertContentEquals(
            expected = expectedKeyHash,
            actual = actual.keyHash,
            message = "Identity should retain the key hash bytes",
        )
    }
}
