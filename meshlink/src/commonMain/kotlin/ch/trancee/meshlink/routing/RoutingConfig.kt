package ch.trancee.meshlink.routing

public data class RoutingConfig(
    public val routeExpiryMillis: Long,
    public val peerTimeoutMillis: Long,
    public val hopLimit: Int,
) {
    init {
        require(routeExpiryMillis > 0) {
            "RoutingConfig routeExpiryMillis must be greater than 0."
        }
        require(peerTimeoutMillis > 0) {
            "RoutingConfig peerTimeoutMillis must be greater than 0."
        }
        require(hopLimit > 0) {
            "RoutingConfig hopLimit must be greater than 0."
        }
    }

    public companion object {
        public fun default(): RoutingConfig {
            return RoutingConfig(
                routeExpiryMillis = 30_000L,
                peerTimeoutMillis = 15_000L,
                hopLimit = 8,
            )
        }
    }
}
