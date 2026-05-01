package ch.trancee.meshlink.routing

import ch.trancee.meshlink.api.PeerIdHex

public data class PeerInfo(
    public val peerId: PeerIdHex,
    public val metric: Int,
    public val lastSeenEpochMillis: Long,
) {
    init {
        require(metric >= 0) {
            "PeerInfo metric must be greater than or equal to 0."
        }
        require(lastSeenEpochMillis >= 0) {
            "PeerInfo lastSeenEpochMillis must be greater than or equal to 0."
        }
    }
}
