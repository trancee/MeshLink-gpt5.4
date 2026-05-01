package ch.trancee.meshlink.messaging

/** Delivery-pipeline tuning knobs. */
public data class MessagingConfig(
  public val rateLimitWindowMillis: Long,
  public val maxMessagesPerWindow: Int,
  public val deliveryTimeoutMillis: Long,
  public val maxPendingMessages: Int,
  public val appIdHash: Int,
) {
  init {
    require(rateLimitWindowMillis > 0) {
      "MessagingConfig rateLimitWindowMillis must be greater than 0."
    }
    require(maxMessagesPerWindow > 0) {
      "MessagingConfig maxMessagesPerWindow must be greater than 0."
    }
    require(deliveryTimeoutMillis > 0) {
      "MessagingConfig deliveryTimeoutMillis must be greater than 0."
    }
    require(maxPendingMessages > 0) { "MessagingConfig maxPendingMessages must be greater than 0." }
  }

  public companion object {
    /** Returns the default delivery-pipeline configuration. */
    public fun default(): MessagingConfig {
      return MessagingConfig(
        rateLimitWindowMillis = 1_000L,
        maxMessagesPerWindow = 32,
        deliveryTimeoutMillis = 5_000L,
        maxPendingMessages = 64,
        appIdHash = 0,
      )
    }
  }
}
