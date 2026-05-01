package ch.trancee.meshlink.wire.messages

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class ChunkAckMessageCodecTest {
    @Test
    public fun encodeAndDecode_roundTripChunkAckMessage(): Unit {
        // Arrange
        val expectedTransferId: Long = 0x1112131415161718
        val expectedHighestContiguousChunkIndex: Int = 6
        val expectedSelectiveAckBitmap: ByteArray = byteArrayOf(0x01, 0x00, 0x01)
        val message = ChunkAckMessage(
            transferId = expectedTransferId,
            highestContiguousChunkIndex = expectedHighestContiguousChunkIndex,
            selectiveAckBitmap = expectedSelectiveAckBitmap,
        )

        // Act
        val encoded: ByteArray = ChunkAckMessageCodec.encode(message = message)
        val decoded: ChunkAckMessage = ChunkAckMessageCodec.decode(payload = encoded)

        // Assert
        assertEquals(
            expected = expectedTransferId,
            actual = decoded.transferId,
            message = "ChunkAckMessageCodec should preserve the transfer identifier",
        )
        assertEquals(
            expected = expectedHighestContiguousChunkIndex,
            actual = decoded.highestContiguousChunkIndex,
            message = "ChunkAckMessageCodec should preserve the highest contiguous acknowledged chunk index",
        )
        assertContentEquals(
            expected = expectedSelectiveAckBitmap,
            actual = decoded.selectiveAckBitmap,
            message = "ChunkAckMessageCodec should preserve the selective acknowledgement bitmap",
        )
    }

    @Test
    public fun decode_throwsWhenPayloadHeaderIsIncomplete(): Unit {
        // Arrange
        val payload: ByteArray = byteArrayOf(0x01)

        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            ChunkAckMessageCodec.decode(payload = payload)
        }

        // Assert
        assertEquals(
            expected = "CHUNK_ACK payload must contain transferId and highestContiguousChunkIndex.",
            actual = error.message,
            message = "ChunkAckMessageCodec should reject truncated CHUNK_ACK headers",
        )
    }
}
