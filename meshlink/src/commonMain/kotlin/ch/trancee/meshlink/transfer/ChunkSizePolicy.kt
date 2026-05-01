package ch.trancee.meshlink.transfer

public data class ChunkSizePolicy(
    public val gattChunkSizeBytes: Int,
    public val l2capChunkSizeBytes: Int,
) {
    init {
        require(gattChunkSizeBytes > 0) {
            "ChunkSizePolicy gattChunkSizeBytes must be greater than 0."
        }
        require(l2capChunkSizeBytes > 0) {
            "ChunkSizePolicy l2capChunkSizeBytes must be greater than 0."
        }
    }

    public fun sizeFor(preferL2cap: Boolean): Int {
        return if (preferL2cap) l2capChunkSizeBytes else gattChunkSizeBytes
    }

    public companion object {
        public fun default(): ChunkSizePolicy {
            return ChunkSizePolicy(
                gattChunkSizeBytes = 244,
                l2capChunkSizeBytes = 1024,
            )
        }
    }
}
