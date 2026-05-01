package ch.trancee.meshlink.messaging

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class CutThroughBufferTest {
  @Test
  public fun appendVisitedHop_appendsTheRelayHopToChunk0(): Unit {
    // Arrange
    val buffer = CutThroughBuffer()
    val chunk0 = byteArrayOf(0x01, 0x02)
    val hopPeerId = byteArrayOf(0x0A, 0x0B, 0x0C)

    // Act
    val actual = buffer.appendVisitedHop(chunk0 = chunk0, hopPeerId = hopPeerId)

    // Assert
    assertContentEquals(expected = byteArrayOf(0x01, 0x02, 0x0A, 0x0B, 0x0C), actual = actual)
  }

  @Test
  public fun appendVisitedHop_rejectsEmptyInputs(): Unit {
    // Arrange
    val buffer = CutThroughBuffer()

    // Act
    val emptyChunkError =
      assertFailsWith<IllegalArgumentException> {
        buffer.appendVisitedHop(chunk0 = byteArrayOf(), hopPeerId = byteArrayOf(0x01))
      }
    val emptyHopError =
      assertFailsWith<IllegalArgumentException> {
        buffer.appendVisitedHop(chunk0 = byteArrayOf(0x01), hopPeerId = byteArrayOf())
      }

    // Assert
    assertEquals(
      expected = "CutThroughBuffer chunk0 must not be empty.",
      actual = emptyChunkError.message,
    )
    assertEquals(
      expected = "CutThroughBuffer hopPeerId must not be empty.",
      actual = emptyHopError.message,
    )
  }
}
