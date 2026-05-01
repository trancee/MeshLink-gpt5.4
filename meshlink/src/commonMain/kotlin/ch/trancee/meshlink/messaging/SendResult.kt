package ch.trancee.meshlink.messaging

/** Immediate outcome returned when enqueueing a message for delivery. */
public sealed interface SendResult {
  /** Message was accepted for immediate transmission. */
  public data class Sent(public val messageId: MessageIdKey) : SendResult

  /** Message was kept for later due to a transient condition. */
  public data class Queued(public val messageId: MessageIdKey, public val reason: QueuedReason) :
    SendResult

  /** Message was rejected and will not be retried automatically. */
  public data class Rejected(public val reason: DeliveryFailureReason) : SendResult
}
