package ch.trancee.meshlink.transport

import ch.trancee.meshlink.api.DiagnosticSink
import ch.trancee.meshlink.api.NoOpDiagnosticSink
import ch.trancee.meshlink.api.PeerIdHex
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Android transport façade.
 *
 * It currently delegates to [VirtualMeshTransport] so engine and API layers can be exercised on
 * Android without depending on live platform BLE plumbing yet.
 */
public class AndroidBleTransport(
  localPeerId: PeerIdHex,
  diagnosticSink: DiagnosticSink = NoOpDiagnosticSink,
) : BleTransport {
  private val delegate =
    VirtualMeshTransport(localPeerId = localPeerId, diagnosticSink = diagnosticSink)

  override val isAdvertising: StateFlow<Boolean> = delegate.isAdvertising

  override val receivedFrames: SharedFlow<ByteArray> = delegate.receivedFrames

  /** Test/simulation helper for wiring an in-memory remote peer. */
  public fun attachPeer(peerId: PeerIdHex, transport: VirtualMeshTransport): Unit {
    delegate.attachPeer(peerId = peerId, transport = transport)
  }

  public fun isConnected(peerId: PeerIdHex): Boolean {
    return delegate.isConnected(peerId = peerId)
  }

  override fun connect(peerId: PeerIdHex): Unit {
    delegate.connect(peerId = peerId)
  }

  override fun disconnect(peerId: PeerIdHex): Unit {
    delegate.disconnect(peerId = peerId)
  }

  override fun send(peerId: PeerIdHex, payload: ByteArray): Unit {
    delegate.send(peerId = peerId, payload = payload)
  }

  override fun advertise(enabled: Boolean): Unit {
    delegate.advertise(enabled = enabled)
  }
}
