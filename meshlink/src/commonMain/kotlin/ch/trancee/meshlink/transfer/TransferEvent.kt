package ch.trancee.meshlink.transfer

public sealed interface TransferEvent {
    public data class Started(
        public val transferId: String,
        public val priority: Priority,
    ) : TransferEvent

    public data class Progress(
        public val transferId: String,
        public val acknowledgedBytes: Long,
        public val totalBytes: Long,
    ) : TransferEvent

    public data class Complete(
        public val transferId: String,
        public val totalBytes: Long,
    ) : TransferEvent

    public data class Failed(
        public val transferId: String,
        public val reason: FailureReason,
    ) : TransferEvent
}
