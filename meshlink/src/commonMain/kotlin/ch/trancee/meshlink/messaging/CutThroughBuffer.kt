package ch.trancee.meshlink.messaging

/**
 * Helper for cut-through relay metadata updates.
 *
 * Relays append a self-delimiting hop trail to chunk 0 so downstream nodes can inspect the visited
 * path without re-encoding the original payload bytes.
 */
public class CutThroughBuffer {
  public fun appendVisitedHop(chunk0: ByteArray, hopPeerId: ByteArray): ByteArray {
    require(chunk0.isNotEmpty()) { "CutThroughBuffer chunk0 must not be empty." }
    require(hopPeerId.isNotEmpty()) { "CutThroughBuffer hopPeerId must not be empty." }
    require(hopPeerId.size <= MAX_HOP_ID_BYTES) {
      "CutThroughBuffer hopPeerId must be 255 bytes or smaller."
    }

    return chunk0 + byteArrayOf(hopPeerId.size.toByte()) + hopPeerId
  }

  internal fun visitedHops(chunk0: ByteArray, payloadSizeBytes: Int): List<ByteArray> {
    require(payloadSizeBytes in 0..chunk0.size) {
      "CutThroughBuffer payloadSizeBytes must be between 0 and the chunk size."
    }

    val hops: MutableList<ByteArray> = mutableListOf()
    var offset: Int = payloadSizeBytes
    while (offset < chunk0.size) {
      val hopLength: Int = chunk0[offset].toInt() and 0xFF
      offset += 1
      require(offset + hopLength <= chunk0.size) {
        "CutThroughBuffer encoded hop trail was truncated."
      }
      hops += chunk0.copyOfRange(fromIndex = offset, toIndex = offset + hopLength)
      offset += hopLength
    }
    return hops
  }

  private companion object {
    private const val MAX_HOP_ID_BYTES: Int = 255
  }
}
