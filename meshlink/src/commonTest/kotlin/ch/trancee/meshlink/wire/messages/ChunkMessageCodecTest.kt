package ch.trancee.meshlink.wire.messages

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class ChunkMessageCodecTest {
  @Test
  public fun encodeAndDecode_roundTripChunkMessage(): Unit {
    // Arrange
    val expectedTransferId: Long = 0x0102030405060708
    val expectedChunkIndex: Int = 4
    val expectedPayload: ByteArray = byteArrayOf(0x11, 0x12, 0x13)
    val message =
      ChunkMessage(
        transferId = expectedTransferId,
        chunkIndex = expectedChunkIndex,
        payload = expectedPayload,
      )

    // Act
    val encoded: ByteArray = ChunkMessageCodec.encode(message = message)
    val decoded: ChunkMessage = ChunkMessageCodec.decode(payload = encoded)

    // Assert
    assertEquals(
      expected = expectedTransferId,
      actual = decoded.transferId,
      message = "ChunkMessageCodec should preserve the transfer identifier",
    )
    assertEquals(
      expected = expectedChunkIndex,
      actual = decoded.chunkIndex,
      message = "ChunkMessageCodec should preserve the chunk index",
    )
    assertContentEquals(
      expected = expectedPayload,
      actual = decoded.payload,
      message = "ChunkMessageCodec should preserve the chunk payload bytes",
    )
  }

  @Test
  public fun decode_throwsWhenPayloadHeaderIsIncomplete(): Unit {
    // Arrange
    val payload: ByteArray = byteArrayOf(0x01)

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> { ChunkMessageCodec.decode(payload = payload) }

    // Assert
    assertEquals(
      expected = "CHUNK payload must contain transferId and chunkIndex.",
      actual = error.message,
      message = "ChunkMessageCodec should reject truncated CHUNK headers",
    )
  }
}
