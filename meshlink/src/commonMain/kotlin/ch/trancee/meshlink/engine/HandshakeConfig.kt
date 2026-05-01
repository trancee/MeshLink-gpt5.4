package ch.trancee.meshlink.engine

/** Retry and timeout policy for peer handshakes. */
public data class HandshakeConfig(
  public val timeoutMillis: Long,
  public val maxRetries: Int,
  public val initialRetryDelayMillis: Long,
) {
  init {
    require(timeoutMillis > 0) { "HandshakeConfig timeoutMillis must be greater than 0." }
    require(maxRetries >= 0) { "HandshakeConfig maxRetries must be greater than or equal to 0." }
    require(initialRetryDelayMillis > 0) {
      "HandshakeConfig initialRetryDelayMillis must be greater than 0."
    }
  }

  public companion object {
    /** Returns the default handshake policy. */
    public fun default(): HandshakeConfig {
      return HandshakeConfig(timeoutMillis = 5_000L, maxRetries = 2, initialRetryDelayMillis = 250L)
    }
  }
}
