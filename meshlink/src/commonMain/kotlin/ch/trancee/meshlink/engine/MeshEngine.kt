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
import ch.trancee.meshlink.routing.RoutingConfig as EngineRoutingConfig
import ch.trancee.meshlink.routing.RoutingEngine
import ch.trancee.meshlink.routing.RoutingUpdate
import ch.trancee.meshlink.transport.AdvertisementCodec
import ch.trancee.meshlink.transport.BleTransport
import ch.trancee.meshlink.transport.MeshHashFilter
import ch.trancee.meshlink.wire.WireMessage
import ch.trancee.meshlink.wire.messages.BroadcastMessage
import ch.trancee.meshlink.wire.messages.HandshakeMessage
import ch.trancee.meshlink.wire.messages.HelloMessage
import ch.trancee.meshlink.wire.messages.RoutedMessage
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Default runtime that wires transport, handshake, delivery, peer state, and pseudonym rotation
 * into the public [MeshLinkApi].
 */
public class MeshEngine
private constructor(
  public val config: MeshEngineConfig,
  public val transport: BleTransport,
  public val handshakeManager: NoiseHandshakeManager,
  public val stateManager: MeshStateManager,
  public val pseudonymRotator: PseudonymRotator,
  public val deliveryPipeline: DeliveryPipeline,
  private val diagnosticSink: DiagnosticSink,
) : MeshLinkApi {
  private val routingEngine: RoutingEngine = RoutingEngine(config = EngineRoutingConfig.default())
  private val mutableState = MutableStateFlow(MeshLinkState.UNINITIALIZED)
  private val expectedApplicationIdHash: Int =
    AdvertisementCodec.applicationIdHash(applicationId = config.meshLinkConfig.applicationId)
  private val meshHashFilter: MeshHashFilter = MeshHashFilter()
  private val mutablePeers = MutableStateFlow<List<PeerDetail>>(emptyList())
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

  /**
   * Sends an application payload through the delivery pipeline before touching the transport so
   * rate-limits and capacity checks happen consistently.
   */
  override fun send(peerId: PeerIdHex, payload: ByteArray): Unit {
    val result: SendResult =
      deliveryPipeline.send(
        senderPeerId = ENGINE_SENDER_PEER_ID,
        recipientPeerId = peerId,
        payload = payload,
        nowEpochMillis = 0L,
      )
    if (result is SendResult.Sent) {
      transport.send(peerId = peerId, payload = payload)
    }
  }

  /** Starts a handshake from the local node's perspective. */
  public fun beginHandshake(
    peerId: PeerIdHex,
    role: HandshakeRole,
    payload: ByteArray,
  ): HandshakeMessage {
    return handshakeManager.beginHandshake(peerId = peerId, role = role, payload = payload)
  }

  /** Produces the next outbound handshake frame for an already active conversation. */
  public fun continueHandshake(peerId: PeerIdHex, payload: ByteArray): HandshakeMessage {
    return handshakeManager.createOutboundMessage(peerId = peerId, payload = payload)
  }

  /**
   * Demultiplexes inbound frames to the appropriate subsystem.
   *
   * Handshake frames stay inside the handshake manager while routed and broadcast payloads are
   * surfaced to application consumers as raw bytes.
   */
  public fun receiveInboundMessage(
    peerId: PeerIdHex,
    message: WireMessage,
    handshakeRole: HandshakeRole = HandshakeRole.RESPONDER,
  ): Unit {
    when (message) {
      is HandshakeMessage ->
        handshakeManager.receiveHandshakeMessage(
          peerId = peerId,
          role = handshakeRole,
          message = message,
        )
      is RoutedMessage -> mutableMessages.tryEmit(message.payload.copyOf())
      is BroadcastMessage -> mutableMessages.tryEmit(message.payload.copyOf())
      is HelloMessage -> handleHelloMessage(message = message)
      else -> Unit
    }
  }

  internal fun processRoutingUpdate(update: RoutingUpdate): Boolean {
    return routingEngine.processUpdate(update = update)
  }

  internal fun nextHopFor(destinationPeerId: PeerIdHex): PeerIdHex? {
    return routingEngine.nextHopFor(destinationPeerId = destinationPeerId)
  }

  /** Derives the pseudonym that should be advertised for the given time window. */
  public fun pseudonymAt(identityKey: ByteArray, timestampMillis: Long): ByteArray {
    return pseudonymRotator.pseudonymAt(
      identityKey = identityKey,
      timestampMillis = timestampMillis,
    )
  }

  /** Verifies whether a candidate pseudonym matches the active rotation window. */
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

  private fun handleHelloMessage(message: HelloMessage): Unit {
    val peerId: PeerIdHex = PeerIdHex.fromBytes(message.peerId)
    val accepted: Boolean =
      meshHashFilter.accepts(
        meshHash = message.peerId,
        appIdHash = message.appIdHash,
        expectedAppIdHash = expectedApplicationIdHash,
      )
    if (!accepted) {
      return
    }
    publishPeers(
      peerDetails =
        listOf(
          PeerDetail(
            peerId = peerId,
            state = PeerState.Discovered,
            displayName = null,
            lastSeenEpochMillis = 0L,
          )
        )
    )
  }

  /** Publishes a fresh peer snapshot and emits discovery diagnostics for each entry. */
  public fun publishPeers(peerDetails: List<PeerDetail>): Unit {
    mutablePeers.value = peerDetails
    peerDetails.forEach { peerDetail ->
      diagnosticSink.emit(code = DiagnosticCode.PEER_DISCOVERED) {
        ch.trancee.meshlink.api.DiagnosticPayload.PeerLifecycle(
          peerId = peerDetail.peerId,
          state =
            when (peerDetail.state) {
              // The explicit branch keeps the emitted type obvious at the call site,
              // even though the current implementation forwards the same value.
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

    // Update the transport before publishing the state change so observers do not see
    // RUNNING while advertising is still disabled, or vice versa.
    transport.advertise(enabled = advertisingEnabled)
    mutableState.value = target
    diagnosticSink.emit(code = diagnosticCode)
  }

  public companion object {
    /** Creates a mesh engine using the default crypto provider and diagnostics sink. */
    public fun create(
      config: MeshEngineConfig,
      transport: BleTransport,
      cryptoProvider: CryptoProvider = CryptoProviderFactory.create(),
    ): MeshEngine {
      val diagnosticSink =
        DiagnosticSink.create(
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

    /** Creates a fully wired engine using caller-supplied infrastructure. */
    public fun create(
      config: MeshEngineConfig,
      transport: BleTransport,
      diagnosticSink: DiagnosticSink,
      cryptoProvider: CryptoProvider,
    ): MeshEngine {
      return MeshEngine(
        config = config,
        transport = transport,
        handshakeManager =
          NoiseHandshakeManager(
            diagnosticSink = diagnosticSink,
            trustMode = config.meshLinkConfig.security.trustMode,
          ),
        stateManager = MeshStateManager(),
        pseudonymRotator = PseudonymRotator(cryptoProvider = cryptoProvider),
        deliveryPipeline =
          DeliveryPipeline(config = MessagingConfig.default(), diagnosticSink = diagnosticSink),
        diagnosticSink = diagnosticSink,
      )
    }

    private val ENGINE_SENDER_PEER_ID: PeerIdHex = PeerIdHex(value = "00000000")
  }
}
