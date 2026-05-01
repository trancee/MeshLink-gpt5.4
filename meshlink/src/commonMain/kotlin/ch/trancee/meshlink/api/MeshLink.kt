package ch.trancee.meshlink.api

import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Platform-neutral entry point for creating a MeshLink runtime.
 *
 * The common implementation currently returns a lightweight stub that exercises the public contract
 * and diagnostics pipeline. Platform-specific factories can swap in a full engine while preserving
 * the same API surface.
 */
@OptIn(ExperimentalObjCName::class)
@ObjCName(name = "MeshLinkRuntime", swiftName = "MeshLinkRuntime", exact = true)
public object MeshLink {
  /** Creates a runtime using the library defaults. */
  public fun create(): MeshLinkApi {
    return create(config = MeshLinkConfig.default())
  }

  /** Creates a runtime backed by a default diagnostic sink derived from [config]. */
  public fun create(config: MeshLinkConfig): MeshLinkApi {
    return create(
      config = config,
      diagnosticSink =
        DiagnosticSink.create(
          bufferSize = config.diagnostics.bufferSize,
          redactPeerIds = config.diagnostics.redactPeerIds,
        ),
    )
  }

  /**
   * Creates a runtime with an explicitly supplied diagnostic sink.
   *
   * This overload is useful in tests and host applications that want to route diagnostics into an
   * existing observability pipeline.
   */
  public fun create(config: MeshLinkConfig, diagnosticSink: DiagnosticSink): MeshLinkApi {
    return StubMeshLinkApi(config = config, diagnosticSink = diagnosticSink)
  }
}

/**
 * Minimal in-memory implementation used by the common entry point.
 *
 * It mirrors the public lifecycle and message flows without depending on platform transport or
 * engine wiring.
 */
internal class StubMeshLinkApi(
  private val config: MeshLinkConfig,
  private val diagnosticSink: DiagnosticSink,
) : MeshLinkApi {
  private val mutableState = MutableStateFlow(MeshLinkState.UNINITIALIZED)
  private val mutablePeers = MutableStateFlow<List<PeerDetail>>(value = emptyList())
  private val mutableMessages =
    MutableSharedFlow<ByteArray>(
      replay = 1,
      extraBufferCapacity = 0,
      onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

  override val state: StateFlow<MeshLinkState> = mutableState.asStateFlow()

  override val peers: StateFlow<List<PeerDetail>> = mutablePeers.asStateFlow()

  override val messages: SharedFlow<ByteArray> = mutableMessages.asSharedFlow()

  override val diagnosticEvents: SharedFlow<DiagnosticEvent> = diagnosticSink.diagnosticEvents

  override fun start(): Unit {
    mutableState.value = MeshLinkState.RUNNING
    diagnosticSink.emit(code = DiagnosticCode.ENGINE_STARTED)
  }

  override fun stop(): Unit {
    mutableState.value = MeshLinkState.STOPPED
    diagnosticSink.emit(code = DiagnosticCode.ENGINE_STOPPED)
  }

  override fun pause(): Unit {
    mutableState.value = MeshLinkState.PAUSED
    diagnosticSink.emit(code = DiagnosticCode.ENGINE_PAUSED)
  }

  override fun resume(): Unit {
    mutableState.value = MeshLinkState.RUNNING
    diagnosticSink.emit(code = DiagnosticCode.ENGINE_RESUMED)
  }

  override fun send(peerId: PeerIdHex, payload: ByteArray): Unit {
    // The stub loops sent payloads back into the shared message stream so tests can
    // exercise application-facing delivery behavior without a real transport.
    mutableMessages.tryEmit(payload.copyOf())
    diagnosticSink.emit(code = DiagnosticCode.MESSAGE_SENT) {
      DiagnosticPayload.PeerLifecycle(peerId = peerId, state = PeerState.Connected)
    }
  }

  internal fun publishPeers(peerDetails: List<PeerDetail>): Unit {
    mutablePeers.value = peerDetails
    peerDetails.forEach { peerDetail ->
      diagnosticSink.emit(code = DiagnosticCode.PEER_DISCOVERED) {
        DiagnosticPayload.PeerLifecycle(peerId = peerDetail.peerId, state = peerDetail.state)
      }
    }
  }

  internal fun publishIncomingMessage(payload: ByteArray): Unit {
    mutableMessages.tryEmit(payload.copyOf())
  }
}
