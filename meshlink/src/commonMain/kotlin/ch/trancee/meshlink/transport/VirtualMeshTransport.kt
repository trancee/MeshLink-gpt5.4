package ch.trancee.meshlink.transport

import ch.trancee.meshlink.api.DiagnosticCode
import ch.trancee.meshlink.api.DiagnosticPayload
import ch.trancee.meshlink.api.DiagnosticSink
import ch.trancee.meshlink.api.NoOpDiagnosticSink
import ch.trancee.meshlink.api.PeerIdHex
import ch.trancee.meshlink.api.PeerState
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

public class VirtualMeshTransport(
  private val localPeerId: PeerIdHex,
  private val diagnosticSink: DiagnosticSink = NoOpDiagnosticSink,
) : BleTransport {
  private val attachedPeers: MutableMap<String, VirtualMeshTransport> = mutableMapOf()
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

  public fun attachPeer(peerId: PeerIdHex, transport: VirtualMeshTransport): Unit {
    attachedPeers[peerId.value] = transport
  }

  public fun isConnected(peerId: PeerIdHex): Boolean {
    return peerId.value in connectedPeers
  }

  override fun connect(peerId: PeerIdHex): Unit {
    if (attachedPeers.containsKey(peerId.value)) {
      connectedPeers += peerId.value
      diagnosticSink.emit(code = DiagnosticCode.PEER_DISCOVERED) {
        DiagnosticPayload.PeerLifecycle(peerId = peerId, state = PeerState.Connected)
      }
    }
  }

  override fun disconnect(peerId: PeerIdHex): Unit {
    if (connectedPeers.remove(peerId.value)) {
      diagnosticSink.emit(code = DiagnosticCode.PEER_LOST) {
        DiagnosticPayload.PeerLifecycle(peerId = peerId, state = PeerState.Disconnected)
      }
    }
  }

  override fun send(peerId: PeerIdHex, payload: ByteArray): Unit {
    val remoteTransport: VirtualMeshTransport = attachedPeers[peerId.value] ?: return
    if (peerId.value !in connectedPeers) {
      return
    }

    remoteTransport.receiveFromPeer(remotePeerId = localPeerId, payload = payload)
    diagnosticSink.emit(code = DiagnosticCode.MESSAGE_SENT) {
      DiagnosticPayload.PeerLifecycle(peerId = peerId, state = PeerState.Connected)
    }
  }

  override fun advertise(enabled: Boolean): Unit {
    mutableIsAdvertising.value = enabled
    diagnosticSink.emit(
      code = if (enabled) DiagnosticCode.ENGINE_STARTED else DiagnosticCode.ENGINE_STOPPED
    )
  }

  private fun receiveFromPeer(remotePeerId: PeerIdHex, payload: ByteArray): Unit {
    mutableReceivedFrames.tryEmit(payload.copyOf())
    diagnosticSink.emit(code = DiagnosticCode.MESSAGE_DELIVERED) {
      DiagnosticPayload.PeerLifecycle(peerId = remotePeerId, state = PeerState.Connected)
    }
  }
}
