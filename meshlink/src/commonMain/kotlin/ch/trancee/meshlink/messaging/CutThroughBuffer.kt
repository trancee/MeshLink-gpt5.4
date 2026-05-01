package ch.trancee.meshlink.messaging

/**
 * Minimal helper for cut-through relay metadata updates.
 *
 * The current implementation simply appends the local hop identifier to chunk 0 so downstream nodes
 * can observe the visited-hop chain without re-encoding the payload.
 */
public class CutThroughBuffer {
  public fun appendVisitedHop(chunk0: ByteArray, hopPeerId: ByteArray): ByteArray {
    require(chunk0.isNotEmpty()) { "CutThroughBuffer chunk0 must not be empty." }
    require(hopPeerId.isNotEmpty()) { "CutThroughBuffer hopPeerId must not be empty." }

    return chunk0 + hopPeerId
  }
}
