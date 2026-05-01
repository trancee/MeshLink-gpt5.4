package ch.trancee.meshlink.transfer

import ch.trancee.meshlink.api.PeerIdHex

public class TransferSession(
    public val transferId: String,
    public val recipientPeerId: PeerIdHex,
    public val priority: Priority,
    private val payload: ByteArray,
    private val chunkSizeBytes: Int,
) {
    private val sackTracker: SackTracker

    init {
        require(transferId.isNotBlank()) {
            "TransferSession transferId must not be blank."
        }
        require(payload.isNotEmpty()) {
            "TransferSession payload must not be empty."
        }
        require(chunkSizeBytes > 0) {
            "TransferSession chunkSizeBytes must be greater than 0."
        }
        sackTracker = SackTracker(totalChunks = totalChunkCount(payloadSize = payload.size))
    }

    public fun totalChunks(): Int {
        return sackTracker.totalChunks
    }

    public fun nextChunks(windowSize: Int): List<OutboundChunk> {
        require(windowSize > 0) {
            "TransferSession windowSize must be greater than 0."
        }

        return sackTracker.missingChunks()
            .take(n = windowSize)
            .map { chunkIndex ->
                OutboundChunk(
                    transferId = transferId,
                    chunkIndex = chunkIndex,
                    payload = chunkPayload(chunkIndex = chunkIndex),
                )
            }
    }

    public fun acknowledge(chunkIndex: Int): TransferEvent {
        sackTracker.acknowledge(chunkIndex = chunkIndex)
        val acknowledgedBytes: Long = sackTracker.acknowledgedChunks().sumOf { acknowledgedChunkIndex ->
            chunkPayload(chunkIndex = acknowledgedChunkIndex).size.toLong()
        }

        return if (sackTracker.isComplete()) {
            TransferEvent.Complete(
                transferId = transferId,
                totalBytes = payload.size.toLong(),
            )
        } else {
            TransferEvent.Progress(
                transferId = transferId,
                acknowledgedBytes = acknowledgedBytes,
                totalBytes = payload.size.toLong(),
            )
        }
    }

    public fun resumeOffsetBytes(): Int {
        return ResumeCalculator.resumeOffsetBytes(
            sackTracker = sackTracker,
            chunkSizeBytes = chunkSizeBytes,
            totalBytes = payload.size,
        )
    }

    public fun cancel(): TransferEvent.Failed {
        return TransferEvent.Failed(
            transferId = transferId,
            reason = FailureReason.CANCELLED,
        )
    }

    public fun isComplete(): Boolean {
        return sackTracker.isComplete()
    }

    private fun chunkPayload(chunkIndex: Int): ByteArray {
        val startIndex: Int = chunkIndex * chunkSizeBytes
        val endIndex: Int = minOf(a = startIndex + chunkSizeBytes, b = payload.size)
        return payload.copyOfRange(fromIndex = startIndex, toIndex = endIndex)
    }

    private fun totalChunkCount(payloadSize: Int): Int {
        return (payloadSize + chunkSizeBytes - 1) / chunkSizeBytes
    }
}

public data class OutboundChunk(
    public val transferId: String,
    public val chunkIndex: Int,
    public val payload: ByteArray,
)
