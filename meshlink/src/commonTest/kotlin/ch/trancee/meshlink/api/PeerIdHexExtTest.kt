package ch.trancee.meshlink.api

import kotlin.test.Test
import kotlin.test.assertEquals

public class PeerIdHexExtTest {
  @Test
  public fun fromBytes_producesLowercaseHexString(): Unit {
    // Arrange
    val bytes: ByteArray = byteArrayOf(0x0A, 0x0B, 0x0C)

    // Act
    val actual: PeerIdHex = PeerIdHex.fromBytes(bytes = bytes)

    // Assert
    assertEquals(
      expected = "0a0b0c",
      actual = actual.value,
      message = "PeerIdHex.fromBytes should normalize output to lowercase hexadecimal",
    )
  }
}
