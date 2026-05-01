package ch.trancee.meshlink.power

public data class PeerKey(
    public val value: String,
) {
    init {
        require(value.isNotBlank()) {
            "PeerKey value must not be blank."
        }
    }
}
