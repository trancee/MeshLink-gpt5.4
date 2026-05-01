package ch.trancee.meshlink.api

public sealed interface DiagnosticPayload {
    public data object None : DiagnosticPayload

    public data class HandshakeFailure(
        public val peerId: PeerIdHex,
        public val reason: String,
    ) : DiagnosticPayload

    public data class TransferProgress(
        public val transferId: String,
        public val bytesTransferred: Long,
        public val totalBytes: Long,
    ) : DiagnosticPayload

    public data class PeerLifecycle(
        public val peerId: PeerIdHex,
        public val state: PeerState,
    ) : DiagnosticPayload

    public data class RoutingChange(
        public val destinationPeerId: PeerIdHex,
        public val metric: Int,
    ) : DiagnosticPayload

    public data class BufferPressure(
        public val usedBytes: Int,
        public val droppedEvents: Int,
    ) : DiagnosticPayload

    public data class PowerTierChanged(
        public val previousTier: String,
        public val currentTier: String,
    ) : DiagnosticPayload

    public data class InternalError(
        public val message: String,
    ) : DiagnosticPayload
}
