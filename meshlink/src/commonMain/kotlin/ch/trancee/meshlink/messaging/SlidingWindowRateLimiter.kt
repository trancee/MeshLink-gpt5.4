package ch.trancee.meshlink.messaging

public class SlidingWindowRateLimiter(
    private val windowMillis: Long,
    private val maxMessagesPerWindow: Int,
) {
    private val timestampsByPeerPair: MutableMap<PeerPair, MutableList<Long>> = mutableMapOf()

    init {
        require(windowMillis > 0) {
            "SlidingWindowRateLimiter windowMillis must be greater than 0."
        }
        require(maxMessagesPerWindow > 0) {
            "SlidingWindowRateLimiter maxMessagesPerWindow must be greater than 0."
        }
    }

    public fun tryAcquire(
        peerPair: PeerPair,
        nowEpochMillis: Long,
    ): Boolean {
        require(nowEpochMillis >= 0) {
            "SlidingWindowRateLimiter nowEpochMillis must be greater than or equal to 0."
        }

        val timestamps: MutableList<Long> = timestampsByPeerPair.getOrPut(peerPair) { mutableListOf() }
        timestamps.removeAll { timestamp -> nowEpochMillis - timestamp >= windowMillis }
        if (timestamps.size >= maxMessagesPerWindow) {
            return false
        }

        timestamps += nowEpochMillis
        return true
    }

    public fun inFlightCount(peerPair: PeerPair): Int {
        return timestampsByPeerPair[peerPair]?.size ?: 0
    }
}
