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
 * [VirtualMeshTransport] bridge, while the transport owns capability-aware L2CAP to GATT fallback
 * decisions instead of delegating the full implementation to a virtual transport instance.
 */
public class AndroidBleTransport(
  private val localPeerId: PeerIdHex,
  private val diagnosticSink: DiagnosticSink = NoOpDiagnosticSink,
) : BleTransport {
  private val attachedAndroidPeers: MutableMap<String, AndroidBleTransport> = mutableMapOf()
  private val attachedVirtualPeers: MutableMap<String, VirtualMeshTransport> = mutableMapOf()
  private val connectedPeers: MutableSet<String> = mutableSetOf()
  private val activeDataPaths: MutableMap<String, TransportDataPath> = mutableMapOf()
  private val l2capProbeCache: OemL2capProbeCache = OemL2capProbeCache()
  private val mutableIsAdvertising = MutableStateFlow(false)
  private val mutableReceivedFrames =
    MutableSharedFlow<ByteArray>(
      replay = 1,
      extraBufferCapacity = 0,
      onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
  private var deviceModel: String = DEFAULT_DEVICE_MODEL
  private var supportsL2cap: Boolean = true
  private val nowEpochMillis: Long = 0L

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

  internal fun configureTransportCapabilities(deviceModel: String, supportsL2cap: Boolean): Unit {
    this.deviceModel = deviceModel
    this.supportsL2cap = supportsL2cap
  }

  internal fun activeDataPath(peerId: PeerIdHex): TransportDataPath? {
    return activeDataPaths[peerId.value]
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
      val selectedDataPath: TransportDataPath =
        negotiateDataPath(
          peerId = peerId,
          remoteDeviceModel = androidPeer.deviceModel,
          remoteSupportsL2cap = androidPeer.supportsL2cap,
        )
      connectedPeers += peerId.value
      activeDataPaths[peerId.value] = selectedDataPath
      androidPeer.onPeerConnected(peerId = localPeerId, dataPath = selectedDataPath)
      return
    }

    val virtualPeer: VirtualMeshTransport = attachedVirtualPeers[peerId.value] ?: return
    if (!virtualPeer.isAdvertising.value) {
      return
    }
    connectedPeers += peerId.value
    activeDataPaths[peerId.value] = TransportDataPath.GATT
    virtualPeer.connect(peerId = localPeerId)
  }

  override fun disconnect(peerId: PeerIdHex): Unit {
    if (!connectedPeers.remove(peerId.value)) {
      return
    }

    activeDataPaths.remove(peerId.value)
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

  private fun negotiateDataPath(
    peerId: PeerIdHex,
    remoteDeviceModel: String,
    remoteSupportsL2cap: Boolean,
  ): TransportDataPath {
    val cachedCapability: OemL2capProbeResult? =
      l2capProbeCache.probe(deviceModel = remoteDeviceModel, nowEpochMillis = nowEpochMillis)
    val preferredDataPath: TransportDataPath =
      ConnectionInitiationPolicy.preferredDataPath(
        preferL2cap = supportsL2cap,
        cachedCapability = cachedCapability,
      )

    return if (preferredDataPath == TransportDataPath.L2CAP && !remoteSupportsL2cap) {
      l2capProbeCache.recordProbe(
        deviceModel = remoteDeviceModel,
        supportsL2cap = false,
        observedAtEpochMillis = nowEpochMillis,
      )
      ConnectionInitiationPolicy.fallbackDataPath(failedDataPath = preferredDataPath)
    } else {
      if (preferredDataPath == TransportDataPath.L2CAP) {
        l2capProbeCache.recordProbe(
          deviceModel = remoteDeviceModel,
          supportsL2cap = true,
          observedAtEpochMillis = nowEpochMillis,
        )
      }
      preferredDataPath
    }
  }

  private fun onPeerConnected(peerId: PeerIdHex, dataPath: TransportDataPath): Unit {
    connectedPeers += peerId.value
    activeDataPaths[peerId.value] = dataPath
  }

  private fun onPeerDisconnected(peerId: PeerIdHex): Unit {
    connectedPeers.remove(peerId.value)
    activeDataPaths.remove(peerId.value)
  }

  private fun receiveFromPeer(peerId: PeerIdHex, payload: ByteArray): Unit {
    mutableReceivedFrames.tryEmit(payload.copyOf())
  }

  private companion object {
    private const val DEFAULT_DEVICE_MODEL: String = "android-generic"
  }
}
