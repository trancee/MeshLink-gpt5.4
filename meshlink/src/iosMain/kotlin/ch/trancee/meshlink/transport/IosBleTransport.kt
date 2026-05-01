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
 * iOS BLE transport façade that models Core Bluetooth central-to-peripheral connection lifecycle.
 *
 * The production shape no longer delegates its full behavior to [VirtualMeshTransport]. Host-side
 * and future simulator tests can still attach in-memory peers for deterministic verification.
 */
public class IosBleTransport(
  private val localPeerId: PeerIdHex,
  private val diagnosticSink: DiagnosticSink = NoOpDiagnosticSink,
) : BleTransport {
  private val attachedIosPeers: MutableMap<String, IosBleTransport> = mutableMapOf()
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

  /** Legacy simulation helper for wiring an in-memory remote peer. */
  public fun attachPeer(peerId: PeerIdHex, transport: VirtualMeshTransport): Unit {
    attachedVirtualPeers[peerId.value] = transport
  }

  /** iOS-test helper for wiring two iOS transports together without virtual delegation. */
  internal fun attachPeer(peerId: PeerIdHex, transport: IosBleTransport): Unit {
    attachedIosPeers[peerId.value] = transport
  }

  public fun isConnected(peerId: PeerIdHex): Boolean {
    return peerId.value in connectedPeers
  }

  override fun connect(peerId: PeerIdHex): Unit {
    val iosPeer: IosBleTransport? = attachedIosPeers[peerId.value]
    if (iosPeer != null) {
      if (!iosPeer.isAdvertising.value) {
        return
      }
      connectedPeers += peerId.value
      iosPeer.onPeerConnected(peerId = localPeerId)
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

    attachedIosPeers[peerId.value]?.onPeerDisconnected(peerId = localPeerId)
    attachedVirtualPeers[peerId.value]?.disconnect(peerId = localPeerId)
  }

  override fun send(peerId: PeerIdHex, payload: ByteArray): Unit {
    if (peerId.value !in connectedPeers) {
      return
    }

    attachedIosPeers[peerId.value]?.receiveFromPeer(peerId = localPeerId, payload = payload)
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
