package ch.trancee.meshlink.api

import kotlin.test.Test
import kotlin.test.assertEquals

public class TrustModeTest {
  @Test
  public fun entries_exposeTofuStrictAndPromptInStableOrder(): Unit {
    // Arrange
    val expected = listOf(TrustMode.TOFU, TrustMode.STRICT, TrustMode.PROMPT)

    // Act
    val actual = TrustMode.entries.toList()

    // Assert
    assertEquals(
      expected = expected,
      actual = actual,
      message = "TrustMode should expose TOFU, STRICT, and PROMPT in declaration order",
    )
  }
}
