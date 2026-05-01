package ch.trancee.meshlink.transport

import ch.trancee.meshlink.api.DiagnosticSink
import ch.trancee.meshlink.api.NoOpDiagnosticSink
import ch.trancee.meshlink.api.PeerIdHex
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android BLE transport façade that models Android-central to peripheral connection lifecycle.
 *
 * Host-side tests can attach either another [AndroidBleTransport] or a legacy
 * [VirtualMeshTransport] bridge, but the transport now owns its own connection state rather than
 * delegating the full implementation to a virtual transport instance.
 */
public class AndroidBleTransport(
  private val localPeerId: PeerIdHex,
  private val diagnosticSink: DiagnosticSink = NoOpDiagnosticSink,
) : BleTransport {
  private val attachedAndroidPeers: MutableMap<String, AndroidBleTransport> = mutableMapOf()
  private val attachedVirtualPeers: MutableMap<String, VirtualMeshTransport> = mutableMapOf()
  private val connectedPeers: MutableSet<String> = mutableSetOf()
  private val mutableIsAdvertising = MutableStateFlow(false)
  private val mutableReceivedFrames =
    MutableSharedFlow<ByteArray>(
      replay = 1,
      extraBufferCapacity = 0,
      onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

  override val isAdvertising: StateFlow<Boolean> = mutableIsAdvertising.asStateFlow()

  override val receivedFrames: SharedFlow<ByteArray> = mutableReceivedFrames.asSharedFlow()

  /** Legacy test/simulation helper for wiring an in-memory remote peer. */
  public fun attachPeer(peerId: PeerIdHex, transport: VirtualMeshTransport): Unit {
    attachedVirtualPeers[peerId.value] = transport
  }

  /** Host-test helper for wiring two Android transports together without a virtual delegate. */
  internal fun attachPeer(peerId: PeerIdHex, transport: AndroidBleTransport): Unit {
    attachedAndroidPeers[peerId.value] = transport
  }

  public fun isConnected(peerId: PeerIdHex): Boolean {
    return peerId.value in connectedPeers
  }

  override fun connect(peerId: PeerIdHex): Unit {
    val androidPeer: AndroidBleTransport? = attachedAndroidPeers[peerId.value]
    if (androidPeer != null) {
      if (!androidPeer.isAdvertising.value) {
        return
      }
      connectedPeers += peerId.value
      androidPeer.onPeerConnected(peerId = localPeerId)
      return
    }

    val virtualPeer: VirtualMeshTransport = attachedVirtualPeers[peerId.value] ?: return
    if (!virtualPeer.isAdvertising.value) {
      return
    }
    connectedPeers += peerId.value
    virtualPeer.connect(peerId = localPeerId)
  }

  override fun disconnect(peerId: PeerIdHex): Unit {
    if (!connectedPeers.remove(peerId.value)) {
      return
    }

    attachedAndroidPeers[peerId.value]?.onPeerDisconnected(peerId = localPeerId)
    attachedVirtualPeers[peerId.value]?.disconnect(peerId = localPeerId)
  }

  override fun send(peerId: PeerIdHex, payload: ByteArray): Unit {
    if (peerId.value !in connectedPeers) {
      return
    }

    attachedAndroidPeers[peerId.value]?.receiveFromPeer(peerId = localPeerId, payload = payload)
    attachedVirtualPeers[peerId.value]?.receiveFromPeer(
      remotePeerId = localPeerId,
      payload = payload,
    )
  }

  override fun advertise(enabled: Boolean): Unit {
    mutableIsAdvertising.value = enabled
  }

  private fun onPeerConnected(peerId: PeerIdHex): Unit {
    connectedPeers += peerId.value
  }

  private fun onPeerDisconnected(peerId: PeerIdHex): Unit {
    connectedPeers.remove(peerId.value)
  }

  private fun receiveFromPeer(peerId: PeerIdHex, payload: ByteArray): Unit {
    mutableReceivedFrames.tryEmit(payload.copyOf())
  }
}
