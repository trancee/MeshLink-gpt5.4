package ch.trancee.meshlink.transfer

import ch.trancee.meshlink.api.PeerIdHex

public class TransferEngine(
    public val config: TransferConfig,
    private val chunkSizePolicy: ChunkSizePolicy,
    private val scheduler: TransferScheduler = TransferScheduler(),
) {
    private val sessionsByTransferId: MutableMap<String, ManagedTransferSession> = linkedMapOf()
    private val inboundChunksByTransferId: MutableMap<String, MutableMap<Int, ByteArray>> = mutableMapOf()
    private val inboundExpectedChunkCounts: MutableMap<String, Int> = mutableMapOf()

    public fun startTransfer(
        transferId: String,
        recipientPeerId: PeerIdHex,
        priority: Priority,
        payload: ByteArray,
        preferL2cap: Boolean,
        nowEpochMillis: Long,
    ): TransferEvent.Started {
        require(nowEpochMillis >= 0) {
            "TransferEngine nowEpochMillis must be greater than or equal to 0."
        }
        require(transferId !in sessionsByTransferId) {
            "TransferEngine already has an active transfer for $transferId."
        }

        val session = TransferSession(
            transferId = transferId,
            recipientPeerId = recipientPeerId,
            priority = priority,
            payload = payload,
            chunkSizeBytes = chunkSizePolicy.sizeFor(preferL2cap = preferL2cap),
        )
        sessionsByTransferId[transferId] = ManagedTransferSession(
            session = session,
            startedAtEpochMillis = nowEpochMillis,
            rateController = ObservationRateController(),
        )
        scheduler.enqueue(transferId = transferId, priority = priority)
        return TransferEvent.Started(
            transferId = transferId,
            priority = priority,
        )
    }

    public fun nextScheduledTransferId(): String? {
        return scheduler.dequeue()
    }

    public fun nextChunks(transferId: String): List<OutboundChunk> {
        return requireManagedTransferSession(transferId = transferId).session.nextChunks(windowSize = config.windowSize)
    }

    public fun acknowledge(
        transferId: String,
        chunkIndex: Int,
        nowEpochMillis: Long,
    ): TransferEvent? {
        require(nowEpochMillis >= 0) {
            "TransferEngine nowEpochMillis must be greater than or equal to 0."
        }

        val managedTransferSession: ManagedTransferSession = sessionsByTransferId[transferId] ?: return null
        managedTransferSession.rateController.recordAcknowledgement(timestampMillis = nowEpochMillis)
        val event: TransferEvent = managedTransferSession.session.acknowledge(chunkIndex = chunkIndex)
        if (event is TransferEvent.Complete) {
            sessionsByTransferId.remove(transferId)
        }
        return event
    }

    public fun cancel(transferId: String): TransferEvent.Failed? {
        val managedTransferSession: ManagedTransferSession = sessionsByTransferId.remove(transferId) ?: return null
        return managedTransferSession.session.cancel()
    }

    public fun failTimedOut(nowEpochMillis: Long): List<TransferEvent.Failed> {
        require(nowEpochMillis >= 0) {
            "TransferEngine nowEpochMillis must be greater than or equal to 0."
        }

        val timedOutTransferIds: List<String> = sessionsByTransferId.entries
            .filter { (_, managedTransferSession) ->
                nowEpochMillis - managedTransferSession.startedAtEpochMillis >= config.timeoutMillis
            }
            .map { (transferId, _) -> transferId }

        return timedOutTransferIds.map { transferId ->
            val managedTransferSession: ManagedTransferSession = sessionsByTransferId.remove(transferId)!!
            TransferEvent.Failed(
                transferId = transferId,
                reason = FailureReason.TIMEOUT,
            )
        }
    }

    public fun receiveChunk(
        transferId: String,
        chunkIndex: Int,
        totalChunks: Int,
        payload: ByteArray,
    ): ByteArray? {
        require(totalChunks > 0) {
            "TransferEngine totalChunks must be greater than 0."
        }
        require(chunkIndex in 0 until totalChunks) {
            "TransferEngine chunkIndex must be between 0 and ${totalChunks - 1}."
        }

        inboundExpectedChunkCounts.getOrPut(transferId) { totalChunks }
        val inboundChunks: MutableMap<Int, ByteArray> = inboundChunksByTransferId.getOrPut(transferId) { linkedMapOf() }
        inboundChunks[chunkIndex] = payload.copyOf()

        return if ((0 until totalChunks).all { expectedChunkIndex -> expectedChunkIndex in inboundChunks }) {
            val reassembledPayload: ByteArray = (0 until totalChunks)
                .flatMap { expectedChunkIndex -> inboundChunks.getValue(expectedChunkIndex).asList() }
                .toByteArray()
            inboundChunksByTransferId.remove(transferId)
            inboundExpectedChunkCounts.remove(transferId)
            reassembledPayload
        } else {
            null
        }
    }

    public fun recommendedDelayMillis(transferId: String): Long? {
        val managedTransferSession: ManagedTransferSession = sessionsByTransferId[transferId] ?: return null
        return managedTransferSession.rateController.recommendedDelayMillis()
    }

    public fun pendingTransfers(): Int {
        return sessionsByTransferId.size
    }

    private fun requireManagedTransferSession(transferId: String): ManagedTransferSession {
        return requireNotNull(sessionsByTransferId[transferId]) {
            "TransferEngine has no active transfer for $transferId."
        }
    }
}

private data class ManagedTransferSession(
    val session: TransferSession,
    val startedAtEpochMillis: Long,
    val rateController: ObservationRateController,
)
