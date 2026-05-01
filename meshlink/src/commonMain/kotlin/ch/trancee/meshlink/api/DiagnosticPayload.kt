package ch.trancee.meshlink.api

/** Typed payload attached to a [DiagnosticEvent]. */
public sealed class DiagnosticPayload {
  /** Returns a payload variant safe to expose when peer identifiers must be redacted. */
  public open fun redactPeerIds(): DiagnosticPayload = this

  /** Diagnostic event without additional structured data. */
  public data object None : DiagnosticPayload()

  /** Failure detail for a handshake attempt. */
  public data class HandshakeFailure(public val peerId: PeerIdHex, public val reason: String) :
    DiagnosticPayload() {
    override fun redactPeerIds(): DiagnosticPayload {
      return copy(peerId = peerId.redacted())
    }
  }

  /** Progress update for a bulk transfer. */
  public data class TransferProgress(
    public val transferId: String,
    public val bytesTransferred: Long,
    public val totalBytes: Long,
  ) : DiagnosticPayload()

  /** Peer lifecycle state attached to a diagnostic. */
  public data class PeerLifecycle(public val peerId: PeerIdHex, public val state: PeerState) :
    DiagnosticPayload() {
    override fun redactPeerIds(): DiagnosticPayload {
      return copy(peerId = peerId.redacted())
    }
  }

  /** Routing-table change summary. */
  public data class RoutingChange(public val destinationPeerId: PeerIdHex, public val metric: Int) :
    DiagnosticPayload() {
    override fun redactPeerIds(): DiagnosticPayload {
      return copy(destinationPeerId = destinationPeerId.redacted())
    }
  }

  /** Buffer saturation or dropped-work signal. */
  public data class BufferPressure(public val usedBytes: Int, public val droppedEvents: Int) :
    DiagnosticPayload()

  /** Power tier transition summary. */
  public data class PowerTierChanged(
    public val previousTier: String,
    public val currentTier: String,
  ) : DiagnosticPayload()

  /** Internal error message intended primarily for diagnostics consumers. */
  public data class InternalError(public val message: String) : DiagnosticPayload()
}

private fun PeerIdHex.redacted(): PeerIdHex {
  return PeerIdHex(value = value.take(n = minOf(a = 8, b = value.length)))
}
