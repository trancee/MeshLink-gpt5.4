package ch.trancee.meshlink.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals

public class HmacSha256Test {
  @Test
  public fun hmacSha256_matchesKnownTestVector(): Unit {
    // Arrange
    val provider = JvmCryptoProvider()
    val key: ByteArray = "key".encodeToByteArray()
    val message: ByteArray = "The quick brown fox jumps over the lazy dog".encodeToByteArray()
    val expected: ByteArray =
      hex("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8")

    // Act
    val actual: ByteArray = provider.hmacSha256(key = key, message = message)

    // Assert
    assertContentEquals(
      expected = expected,
      actual = actual,
      message = "JvmCryptoProvider should match the known HMAC-SHA256 test vector",
    )
  }

  private fun hex(value: String): ByteArray {
    return value.chunked(size = 2).map { chunk -> chunk.toInt(radix = 16).toByte() }.toByteArray()
  }
}
