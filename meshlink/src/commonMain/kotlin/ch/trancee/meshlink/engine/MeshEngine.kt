package ch.trancee.meshlink.engine

import ch.trancee.meshlink.api.DiagnosticCode
import ch.trancee.meshlink.api.DiagnosticEvent
import ch.trancee.meshlink.api.DiagnosticSink
import ch.trancee.meshlink.api.MeshLinkApi
import ch.trancee.meshlink.api.MeshLinkState
import ch.trancee.meshlink.api.PeerDetail
import ch.trancee.meshlink.api.PeerIdHex
import ch.trancee.meshlink.crypto.CryptoProvider
import ch.trancee.meshlink.crypto.CryptoProviderFactory
import ch.trancee.meshlink.transport.BleTransport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

public class MeshEngine private constructor(
    public val config: MeshEngineConfig,
    public val transport: BleTransport,
    public val handshakeManager: NoiseHandshakeManager,
    public val stateManager: MeshStateManager,
    public val pseudonymRotator: PseudonymRotator,
    private val diagnosticSink: DiagnosticSink,
) : MeshLinkApi {
    private val mutableState = MutableStateFlow(MeshLinkState.UNINITIALIZED)
    private val mutablePeers = MutableStateFlow<List<PeerDetail>>(emptyList())

    override val state: StateFlow<MeshLinkState> = mutableState.asStateFlow()

    override val peers: StateFlow<List<PeerDetail>> = mutablePeers.asStateFlow()

    override val messages: SharedFlow<ByteArray> = transport.receivedFrames

    override val diagnosticEvents: SharedFlow<DiagnosticEvent> = diagnosticSink.diagnosticEvents

    override fun start(): Unit {
        transitionTo(
            target = MeshLinkState.RUNNING,
            diagnosticCode = DiagnosticCode.ENGINE_STARTED,
            advertisingEnabled = true,
        )
    }

    override fun stop(): Unit {
        transitionTo(
            target = MeshLinkState.STOPPED,
            diagnosticCode = DiagnosticCode.ENGINE_STOPPED,
            advertisingEnabled = false,
        )
    }

    override fun pause(): Unit {
        transitionTo(
            target = MeshLinkState.PAUSED,
            diagnosticCode = DiagnosticCode.ENGINE_PAUSED,
            advertisingEnabled = false,
        )
    }

    override fun resume(): Unit {
        transitionTo(
            target = MeshLinkState.RUNNING,
            diagnosticCode = DiagnosticCode.ENGINE_RESUMED,
            advertisingEnabled = true,
        )
    }

    override fun send(
        peerId: PeerIdHex,
        payload: ByteArray,
    ): Unit {
        transport.send(peerId = peerId, payload = payload)
    }

    private fun transitionTo(
        target: MeshLinkState,
        diagnosticCode: DiagnosticCode,
        advertisingEnabled: Boolean,
    ): Unit {
        val current: MeshLinkState = mutableState.value
        check(current.canTransitionTo(target = target)) {
            "MeshEngine cannot transition from ${current.name} to ${target.name}."
        }

        transport.advertise(enabled = advertisingEnabled)
        mutableState.value = target
        diagnosticSink.emit(code = diagnosticCode)
    }

    public companion object {
        public fun create(
            config: MeshEngineConfig,
            transport: BleTransport,
            cryptoProvider: CryptoProvider = CryptoProviderFactory.create(),
        ): MeshEngine {
            val diagnosticSink = DiagnosticSink.create(
                bufferSize = config.meshLinkConfig.diagnostics.bufferSize,
                redactPeerIds = config.meshLinkConfig.diagnostics.redactPeerIds,
            )
            return create(
                config = config,
                transport = transport,
                diagnosticSink = diagnosticSink,
                cryptoProvider = cryptoProvider,
            )
        }

        public fun create(
            config: MeshEngineConfig,
            transport: BleTransport,
            diagnosticSink: DiagnosticSink,
            cryptoProvider: CryptoProvider,
        ): MeshEngine {
            return MeshEngine(
                config = config,
                transport = transport,
                handshakeManager = NoiseHandshakeManager(diagnosticSink = diagnosticSink),
                stateManager = MeshStateManager(),
                pseudonymRotator = PseudonymRotator(cryptoProvider = cryptoProvider),
                diagnosticSink = diagnosticSink,
            )
        }
    }
}
