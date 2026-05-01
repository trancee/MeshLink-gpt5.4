package ch.trancee.meshlink.engine

import ch.trancee.meshlink.api.DiagnosticCode
import ch.trancee.meshlink.api.DiagnosticEvent
import ch.trancee.meshlink.api.DiagnosticSink
import ch.trancee.meshlink.api.MeshLinkApi
import ch.trancee.meshlink.api.MeshLinkState
import ch.trancee.meshlink.api.PeerDetail
import ch.trancee.meshlink.api.PeerIdHex
import ch.trancee.meshlink.api.PeerState
import ch.trancee.meshlink.crypto.CryptoProvider
import ch.trancee.meshlink.crypto.CryptoProviderFactory
import ch.trancee.meshlink.crypto.noise.HandshakeRole
import ch.trancee.meshlink.messaging.DeliveryPipeline
import ch.trancee.meshlink.messaging.MessagingConfig
import ch.trancee.meshlink.messaging.SendResult
import ch.trancee.meshlink.transport.BleTransport
import ch.trancee.meshlink.wire.WireMessage
import ch.trancee.meshlink.wire.messages.BroadcastMessage
import ch.trancee.meshlink.wire.messages.HandshakeMessage
import ch.trancee.meshlink.wire.messages.RoutedMessage
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

public class MeshEngine private constructor(
    public val config: MeshEngineConfig,
    public val transport: BleTransport,
    public val handshakeManager: NoiseHandshakeManager,
    public val stateManager: MeshStateManager,
    public val pseudonymRotator: PseudonymRotator,
    public val deliveryPipeline: DeliveryPipeline,
    private val diagnosticSink: DiagnosticSink,
) : MeshLinkApi {
    private val mutableState = MutableStateFlow(MeshLinkState.UNINITIALIZED)
    private val mutablePeers = MutableStateFlow<List<PeerDetail>>(emptyList())
    private val mutableMessages = MutableSharedFlow<ByteArray>(
        replay = 1,
        extraBufferCapacity = 0,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override val state: StateFlow<MeshLinkState> = mutableState.asStateFlow()

    override val peers: StateFlow<List<PeerDetail>> = mutablePeers.asStateFlow()

    override val messages: SharedFlow<ByteArray> = mutableMessages.asSharedFlow()

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
        val result: SendResult = deliveryPipeline.send(
            senderPeerId = ENGINE_SENDER_PEER_ID,
            recipientPeerId = peerId,
            payload = payload,
            nowEpochMillis = 0L,
        )
        if (result is SendResult.Sent) {
            transport.send(peerId = peerId, payload = payload)
        }
    }

    public fun beginHandshake(
        peerId: PeerIdHex,
        role: HandshakeRole,
        payload: ByteArray,
    ): HandshakeMessage {
        return handshakeManager.beginHandshake(
            peerId = peerId,
            role = role,
            payload = payload,
        )
    }

    public fun continueHandshake(
        peerId: PeerIdHex,
        payload: ByteArray,
    ): HandshakeMessage {
        return handshakeManager.createOutboundMessage(
            peerId = peerId,
            payload = payload,
        )
    }

    public fun receiveInboundMessage(
        peerId: PeerIdHex,
        message: WireMessage,
        handshakeRole: HandshakeRole = HandshakeRole.RESPONDER,
    ): Unit {
        when (message) {
            is HandshakeMessage -> handshakeManager.receiveHandshakeMessage(
                peerId = peerId,
                role = handshakeRole,
                message = message,
            )
            is RoutedMessage -> mutableMessages.tryEmit(message.payload.copyOf())
            is BroadcastMessage -> mutableMessages.tryEmit(message.payload.copyOf())
            else -> Unit
        }
    }

    public fun pseudonymAt(
        identityKey: ByteArray,
        timestampMillis: Long,
    ): ByteArray {
        return pseudonymRotator.pseudonymAt(
            identityKey = identityKey,
            timestampMillis = timestampMillis,
        )
    }

    public fun verifyPseudonym(
        candidate: ByteArray,
        identityKey: ByteArray,
        timestampMillis: Long,
    ): Boolean {
        return pseudonymRotator.isValidForCurrentWindow(
            candidate = candidate,
            identityKey = identityKey,
            timestampMillis = timestampMillis,
        )
    }

    public fun publishPeers(peerDetails: List<PeerDetail>): Unit {
        mutablePeers.value = peerDetails
        peerDetails.forEach { peerDetail ->
            diagnosticSink.emit(code = DiagnosticCode.PEER_DISCOVERED) {
                ch.trancee.meshlink.api.DiagnosticPayload.PeerLifecycle(
                    peerId = peerDetail.peerId,
                    state = when (peerDetail.state) {
                        PeerState.Disconnected -> PeerState.Disconnected
                        else -> peerDetail.state
                    },
                )
            }
        }
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
                deliveryPipeline = DeliveryPipeline(
                    config = MessagingConfig.default(),
                    diagnosticSink = diagnosticSink,
                ),
                diagnosticSink = diagnosticSink,
            )
        }

        private val ENGINE_SENDER_PEER_ID: PeerIdHex = PeerIdHex(value = "00000000")
    }
}
