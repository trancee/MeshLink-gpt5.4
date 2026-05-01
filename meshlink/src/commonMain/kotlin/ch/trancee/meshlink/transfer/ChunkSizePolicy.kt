package ch.trancee.meshlink.transfer

/** Transport-aware chunk sizes for bulk transfer. */
public data class ChunkSizePolicy(
  public val gattChunkSizeBytes: Int,
  public val l2capChunkSizeBytes: Int,
) {
  init {
    require(gattChunkSizeBytes > 0) { "ChunkSizePolicy gattChunkSizeBytes must be greater than 0." }
    require(l2capChunkSizeBytes > 0) {
      "ChunkSizePolicy l2capChunkSizeBytes must be greater than 0."
    }
  }

  /** Returns the chunk size for the selected transport preference. */
  public fun sizeFor(preferL2cap: Boolean): Int {
    return if (preferL2cap) l2capChunkSizeBytes else gattChunkSizeBytes
  }

  public companion object {
    /** Returns the default chunk sizes for GATT and L2CAP. */
    public fun default(): ChunkSizePolicy {
      return ChunkSizePolicy(gattChunkSizeBytes = 244, l2capChunkSizeBytes = 1024)
    }
  }
}
