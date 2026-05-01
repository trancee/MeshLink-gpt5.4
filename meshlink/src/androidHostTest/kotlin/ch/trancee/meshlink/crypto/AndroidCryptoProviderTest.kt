package ch.trancee.meshlink.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class AndroidCryptoProviderTest {
  @Test
  public fun generateEd25519KeyPair_throwsHelpfulPlaceholderMessage(): Unit {
    // Arrange
    val provider = AndroidCryptoProvider()

    // Act
    val error = assertFailsWith<UnsupportedOperationException> { provider.generateEd25519KeyPair() }

    // Assert
    assertEquals(
      expected = "CryptoProviderFactory has not been wired to a platform crypto backend yet.",
      actual = error.message,
      message =
        "AndroidCryptoProvider should surface that the Android backend is not implemented yet",
    )
  }
}
