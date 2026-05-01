package ch.trancee.meshlink.messaging

public class CutThroughBuffer {
  public fun appendVisitedHop(chunk0: ByteArray, hopPeerId: ByteArray): ByteArray {
    require(chunk0.isNotEmpty()) { "CutThroughBuffer chunk0 must not be empty." }
    require(hopPeerId.isNotEmpty()) { "CutThroughBuffer hopPeerId must not be empty." }

    return chunk0 + hopPeerId
  }
}
