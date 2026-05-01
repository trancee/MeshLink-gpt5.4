package ch.trancee.meshlink.transfer

/** Observable transfer lifecycle event. */
public sealed interface TransferEvent {
  /** Transfer was accepted for scheduling. */
  public data class Started(public val transferId: String, public val priority: Priority) :
    TransferEvent

  /** Transfer acknowledgement progress advanced. */
  public data class Progress(
    public val transferId: String,
    public val acknowledgedBytes: Long,
    public val totalBytes: Long,
  ) : TransferEvent

  /** Transfer completed successfully. */
  public data class Complete(public val transferId: String, public val totalBytes: Long) :
    TransferEvent

  /** Transfer terminated with failure. */
  public data class Failed(public val transferId: String, public val reason: FailureReason) :
    TransferEvent
}
