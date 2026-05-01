package ch.trancee.meshlink.transfer

public object ResumeCalculator {
    public fun resumeOffsetBytes(
        sackTracker: SackTracker,
        chunkSizeBytes: Int,
        totalBytes: Int,
    ): Int {
        require(chunkSizeBytes > 0) {
            "ResumeCalculator chunkSizeBytes must be greater than 0."
        }
        require(totalBytes >= 0) {
            "ResumeCalculator totalBytes must be greater than or equal to 0."
        }

        val highestContiguousChunkIndex: Int = sackTracker.highestContiguousAcknowledgedChunkIndex() ?: return 0
        val contiguousBytes: Int = (highestContiguousChunkIndex + 1) * chunkSizeBytes
        return contiguousBytes.coerceAtMost(maximumValue = totalBytes)
    }
}
