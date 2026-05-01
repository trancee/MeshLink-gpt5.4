package ch.trancee.meshlink.transfer

public data class TransferConfig(
    public val timeoutMillis: Long,
    public val retransmitLimit: Int,
    public val windowSize: Int,
) {
    init {
        require(timeoutMillis > 0) {
            "TransferConfig timeoutMillis must be greater than 0."
        }
        require(retransmitLimit >= 0) {
            "TransferConfig retransmitLimit must be greater than or equal to 0."
        }
        require(windowSize > 0) {
            "TransferConfig windowSize must be greater than 0."
        }
    }

    public companion object {
        public fun default(): TransferConfig {
            return TransferConfig(
                timeoutMillis = 5_000L,
                retransmitLimit = 3,
                windowSize = 8,
            )
        }
    }
}
