package ch.trancee.meshlink.messaging

public sealed interface SendResult {
  public data class Sent(public val messageId: MessageIdKey) : SendResult

  public data class Queued(public val messageId: MessageIdKey, public val reason: QueuedReason) :
    SendResult

  public data class Rejected(public val reason: DeliveryFailureReason) : SendResult
}
