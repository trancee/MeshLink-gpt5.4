package ch.trancee.meshlink.transfer

public class SackTracker(public val totalChunks: Int) {
  private val acknowledgedChunkIndices: MutableSet<Int> = linkedSetOf()

  init {
    require(totalChunks > 0) { "SackTracker totalChunks must be greater than 0." }
  }

  public fun acknowledge(chunkIndex: Int): Unit {
    require(chunkIndex in 0 until totalChunks) {
      "SackTracker chunkIndex must be between 0 and ${totalChunks - 1}."
    }
    acknowledgedChunkIndices += chunkIndex
  }

  public fun isAcknowledged(chunkIndex: Int): Boolean {
    require(chunkIndex in 0 until totalChunks) {
      "SackTracker chunkIndex must be between 0 and ${totalChunks - 1}."
    }
    return chunkIndex in acknowledgedChunkIndices
  }

  public fun acknowledgedChunks(): Set<Int> {
    return acknowledgedChunkIndices.toSet()
  }

  public fun missingChunks(): List<Int> {
    return (0 until totalChunks).filter { chunkIndex -> chunkIndex !in acknowledgedChunkIndices }
  }

  public fun highestContiguousAcknowledgedChunkIndex(): Int? {
    if (0 !in acknowledgedChunkIndices) {
      return null
    }

    var currentChunkIndex: Int = 0
    while (currentChunkIndex + 1 in acknowledgedChunkIndices) {
      currentChunkIndex += 1
    }
    return currentChunkIndex
  }

  public fun isComplete(): Boolean {
    return acknowledgedChunkIndices.size == totalChunks
  }
}
