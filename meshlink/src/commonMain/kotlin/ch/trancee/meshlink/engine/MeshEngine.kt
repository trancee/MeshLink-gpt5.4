package ch.trancee.meshlink.engine

import ch.trancee.meshlink.api.DiagnosticCode
import ch.trancee.meshlink.api.DiagnosticEvent
import ch.trancee.meshlink.api.DiagnosticPayload
import ch.trancee.meshlink.api.DiagnosticSink
import ch.trancee.meshlink.api.MeshHealthSnapshot
import ch.trancee.meshlink.api.MeshLinkApi
import ch.trancee.meshlink.api.MeshLinkState
import ch.trancee.meshlink.api.PeerDetail
import ch.trancee.meshlink.api.PeerIdHex
import ch.trancee.meshlink.api.PeerState
import ch.trancee.meshlink.crypto.CryptoProvider
import ch.trancee.meshlink.crypto.CryptoProviderFactory
import ch.trancee.meshlink.crypto.TrustDecision
import ch.trancee.meshlink.crypto.noise.HandshakeRole
import ch.trancee.meshlink.messaging.DeliveryPipeline
import ch.trancee.meshlink.messaging.MessagingConfig
import ch.trancee.meshlink.messaging.SendResult
import ch.trancee.meshlink.power.FixedBatteryMonitor
import ch.trancee.meshlink.power.ManagedConnection
import ch.trancee.meshlink.power.PeerKey
import ch.trancee.meshlink.power.PowerConfig
import ch.trancee.meshlink.power.PowerDecision
import ch.trancee.meshlink.power.PowerManager
import ch.trancee.meshlink.power.TransferStatus
import ch.trancee.meshlink.routing.RouteEntry
import ch.trancee.meshlink.routing.RoutingConfig as EngineRoutingConfig
import ch.trancee.meshlink.routing.RoutingEngine
import ch.trancee.meshlink.routing.RoutingUpdate
import ch.trancee.meshlink.transfer.ChunkSizePolicy
import ch.trancee.meshlink.transfer.OutboundChunk
import ch.trancee.meshlink.transfer.Priority
import ch.trancee.meshlink.transfer.TransferConfig
import ch.trancee.meshlink.transfer.TransferEngine
import ch.trancee.meshlink.transfer.TransferEvent
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
  internal val sessionRegistry: MeshSessionRegistry = MeshSessionRegistry()
  internal val transferEngine: TransferEngine =
    TransferEngine(
      config = TransferConfig.default(),
      chunkSizePolicy =
        ChunkSizePolicy(
          gattChunkSizeBytes = config.meshLinkConfig.transfers.chunkSizeBytes,
          l2capChunkSizeBytes = config.meshLinkConfig.transfers.chunkSizeBytes,
        ),
    )
  internal val batteryMonitor: FixedBatteryMonitor =
    FixedBatteryMonitor(initialBatteryPercent = 100)
  internal val powerManager: PowerManager =
    PowerManager(batteryMonitor = batteryMonitor, config = PowerConfig.default())
  private val activeTransferIdsByPeer: MutableMap<String, MutableSet<String>> = linkedMapOf()
  private val recipientPeerIdsByTransferId: MutableMap<String, PeerIdHex> = linkedMapOf()
  private var currentPowerDecision: PowerDecision = powerManager.evaluate(connections = emptyList())
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
    val nextHopPeerId: PeerIdHex = nextHopFor(destinationPeerId = peerId) ?: peerId
    val result: SendResult =
      deliveryPipeline.send(
        senderPeerId = ENGINE_SENDER_PEER_ID,
        recipientPeerId = peerId,
        payload = payload,
        nowEpochMillis = 0L,
      )
    if (result is SendResult.Sent) {
      transport.send(peerId = nextHopPeerId, payload = payload)
    }
    syncSession(peerId = peerId)
  }

  override fun healthSnapshot(): MeshHealthSnapshot {
    val connectedPeers: List<PeerDetail> =
      mutablePeers.value.filter { peerDetail ->
        peerDetail.state == PeerState.Connected || peerDetail.state == PeerState.Connecting
      }
    return MeshHealthSnapshot(
      connectedPeers = connectedPeers,
      routingTableSize = routingEngine.destinations().size,
      activeTransferCount = sessionRegistry.activeTransferCount(),
      bufferedMessageCount = deliveryPipeline.pendingCount() + deliveryPipeline.bufferedCount(),
      powerTier = currentPowerDecision.tier,
    )
  }

  override fun forgetPeer(peerId: PeerIdHex): Unit {
    mutablePeers.value = mutablePeers.value.filterNot { peerDetail -> peerDetail.peerId == peerId }
    deliveryPipeline.clearPeer(recipientPeerId = peerId)
    activeTransferIdsByPeer[peerId.value]?.toSet()?.forEach { transferId ->
      cancelTransfer(transferId = transferId)
    }
    clearRoutesForPeer(peerId = peerId)
    transport.disconnect(peerId = peerId)
    sessionRegistry.remove(peerId = peerId)
    reevaluatePowerDecision()
  }

  override fun factoryReset(): Unit {
    check(state.value == MeshLinkState.STOPPED) {
      "MeshEngine must be stopped before factoryReset()."
    }

    mutablePeers.value = emptyList()
    deliveryPipeline.reset()
    transferEngine.reset()
    activeTransferIdsByPeer.clear()
    recipientPeerIdsByTransferId.clear()
    clearAllRoutes()
    sessionRegistry.clear()
    reevaluatePowerDecision()
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
      is HandshakeMessage -> {
        handshakeManager.receiveHandshakeMessage(
          peerId = peerId,
          role = handshakeRole,
          message = message,
        )
        syncSession(peerId = peerId)
      }
      is RoutedMessage -> mutableMessages.tryEmit(message.payload.copyOf())
      is BroadcastMessage -> mutableMessages.tryEmit(message.payload.copyOf())
      is HelloMessage -> handleHelloMessage(message = message)
      else -> Unit
    }
  }

  internal fun processRoutingUpdate(update: RoutingUpdate, nowEpochMillis: Long = 0L): Boolean {
    val accepted: Boolean = routingEngine.processUpdate(update = update)
    syncSession(peerId = update.destinationPeerId)
    if (!accepted) {
      reevaluatePowerDecision()
      return false
    }

    val nextHopPeerId: PeerIdHex? = nextHopFor(destinationPeerId = update.destinationPeerId)
    if (nextHopPeerId != null) {
      deliveryPipeline
        .flushBuffered(recipientPeerId = update.destinationPeerId, nowEpochMillis = nowEpochMillis)
        .forEach { sendResult ->
          if (sendResult is SendResult.Sent) {
            val payload: ByteArray =
              requireNotNull(deliveryPipeline.payloadFor(messageId = sendResult.messageId)) {
                "MeshEngine lost buffered payload ${sendResult.messageId.sequenceNumber}."
              }
            transport.send(peerId = nextHopPeerId, payload = payload)
          }
        }
    }
    reevaluatePowerDecision()
    return true
  }

  internal fun nextHopFor(destinationPeerId: PeerIdHex): PeerIdHex? {
    return routingEngine.nextHopFor(destinationPeerId = destinationPeerId)
  }

  internal fun sendRouted(peerId: PeerIdHex, payload: ByteArray, nowEpochMillis: Long): SendResult {
    val nextHopPeerId: PeerIdHex? = nextHopFor(destinationPeerId = peerId)
    if (nextHopPeerId == null) {
      val queued: SendResult =
        deliveryPipeline.bufferForUnavailableRoute(
          senderPeerId = ENGINE_SENDER_PEER_ID,
          recipientPeerId = peerId,
          payload = payload,
          nowEpochMillis = nowEpochMillis,
        )
      syncSession(peerId = peerId)
      return queued
    }

    val sent: SendResult =
      deliveryPipeline.send(
        senderPeerId = ENGINE_SENDER_PEER_ID,
        recipientPeerId = peerId,
        payload = payload,
        nowEpochMillis = nowEpochMillis,
      )
    if (sent is SendResult.Sent) {
      transport.send(peerId = nextHopPeerId, payload = payload)
    }
    syncSession(peerId = peerId)
    return sent
  }

  internal fun startTransfer(
    transferId: String,
    recipientPeerId: PeerIdHex,
    priority: Priority,
    payload: ByteArray,
    preferL2cap: Boolean,
    nowEpochMillis: Long,
  ): TransferEvent.Started {
    val event: TransferEvent.Started =
      transferEngine.startTransfer(
        transferId = transferId,
        recipientPeerId = recipientPeerId,
        priority = priority,
        payload = payload,
        preferL2cap = preferL2cap,
        nowEpochMillis = nowEpochMillis,
      )
    recipientPeerIdsByTransferId[transferId] = recipientPeerId
    activeTransferIdsByPeer.getOrPut(recipientPeerId.value) { linkedSetOf() } += transferId
    syncSession(peerId = recipientPeerId)
    reevaluatePowerDecision()
    return event
  }

  internal fun nextTransferChunks(transferId: String): List<OutboundChunk> {
    return transferEngine.nextChunks(transferId = transferId)
  }

  internal fun acknowledgeTransfer(
    transferId: String,
    chunkIndex: Int,
    nowEpochMillis: Long,
  ): TransferEvent? {
    val event: TransferEvent? =
      transferEngine.acknowledge(
        transferId = transferId,
        chunkIndex = chunkIndex,
        nowEpochMillis = nowEpochMillis,
      )
    if (event is TransferEvent.Complete || event is TransferEvent.Failed) {
      clearTransferTracking(transferId = transferId)
    }
    return event
  }

  internal fun cancelTransfer(transferId: String): TransferEvent.Failed? {
    val cancelled: TransferEvent.Failed? = transferEngine.cancel(transferId = transferId)
    if (cancelled != null) {
      clearTransferTracking(transferId = transferId)
    }
    return cancelled
  }

  internal fun sweepState(nowEpochMillis: Long): MeshStateSweepResult {
    val sweepResult: MeshStateSweepResult =
      stateManager.sweep(
        peers =
          mutablePeers.value.map { peerDetail ->
            ManagedPeer(
              peerId = peerDetail.peerId,
              lastSeenEpochMillis = peerDetail.lastSeenEpochMillis,
            )
          },
        routes =
          routingEngine.destinations().flatMap { destinationPeerId ->
            routingEngine.routesFor(destinationPeerId = destinationPeerId).map { route ->
              ManagedRoute(
                destinationPeerId = route.destinationPeerId,
                expiresAtEpochMillis = route.expiresAtEpochMillis,
              )
            }
          },
        nowEpochMillis = nowEpochMillis,
      )

    if (sweepResult.stalePeers.isNotEmpty()) {
      val stalePeerIds: Set<String> = sweepResult.stalePeers.mapTo(linkedSetOf()) { it.value }
      mutablePeers.value =
        mutablePeers.value.filterNot { peerDetail -> peerDetail.peerId.value in stalePeerIds }
      sweepResult.stalePeers.forEach { peerId ->
        syncSession(peerId = peerId, transportConnected = false)
      }
    }

    sweepResult.expiredRoutes.forEach { destinationPeerId ->
      routingEngine
        .routesFor(destinationPeerId = destinationPeerId)
        .filter { route -> route.expiresAtEpochMillis <= nowEpochMillis }
        .forEach { route -> withdrawRoute(route = route, nowEpochMillis = nowEpochMillis) }
      syncSession(peerId = destinationPeerId)
    }

    transferEngine.failTimedOut(nowEpochMillis = nowEpochMillis).forEach { failedEvent ->
      clearTransferTracking(transferId = failedEvent.transferId)
    }
    deliveryPipeline.failTimedOut(nowEpochMillis = nowEpochMillis)
    reevaluatePowerDecision()
    return sweepResult
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
    val publishedPeerIds: Set<String> =
      peerDetails.mapTo(linkedSetOf()) { peerDetail -> peerDetail.peerId.value }
    sessionRegistry
      .snapshot()
      .map { record -> record.peerId }
      .forEach { peerId ->
        if (peerId.value !in publishedPeerIds) {
          syncSession(peerId = peerId, transportConnected = false)
        }
      }
    peerDetails.forEach { peerDetail ->
      syncSession(
        peerId = peerDetail.peerId,
        transportConnected =
          peerDetail.state == PeerState.Connected || peerDetail.state == PeerState.Connecting,
      )
      diagnosticSink.emit(code = DiagnosticCode.PEER_DISCOVERED) {
        DiagnosticPayload.PeerLifecycle(
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
    reevaluatePowerDecision()
  }

  private fun syncSession(peerId: PeerIdHex, transportConnected: Boolean? = null): Unit {
    val existing: MeshSessionRecord? = sessionRegistry.session(peerId = peerId)
    val effectiveTransportConnected: Boolean =
      transportConnected
        ?: existing?.transportConnected
        ?: mutablePeers.value.any { peerDetail ->
          peerDetail.peerId == peerId &&
            (peerDetail.state == PeerState.Connected || peerDetail.state == PeerState.Connecting)
        }
    val effectiveTrustDecision: TrustDecision? =
      existing?.trustDecision
        ?: if (handshakeManager.session(peerId = peerId) != null) {
          TrustDecision.Accepted
        } else {
          null
        }
    val effectiveRouteAvailable: Boolean = nextHopFor(destinationPeerId = peerId) != null
    val effectiveTransferIds: Set<String> =
      activeTransferIdsByPeer[peerId.value]?.toSet() ?: emptySet()

    if (
      !shouldRetainSession(
        transportConnected = effectiveTransportConnected,
        trustDecision = effectiveTrustDecision,
        routeAvailable = effectiveRouteAvailable,
        activeTransferIds = effectiveTransferIds,
      )
    ) {
      sessionRegistry.remove(peerId = peerId)
      return
    }

    sessionRegistry.upsert(
      peerId = peerId,
      transportConnected = effectiveTransportConnected,
      trustDecision = effectiveTrustDecision,
      routeAvailable = effectiveRouteAvailable,
      activeTransferIds = effectiveTransferIds,
    )
  }

  private fun clearRoutesForPeer(peerId: PeerIdHex): Unit {
    routingEngine.routesFor(destinationPeerId = peerId).forEach { route ->
      withdrawRoute(route = route, nowEpochMillis = 0L)
    }
  }

  private fun clearAllRoutes(): Unit {
    routingEngine.destinations().forEach { destinationPeerId ->
      clearRoutesForPeer(peerId = destinationPeerId)
    }
  }

  private fun shouldRetainSession(
    transportConnected: Boolean,
    trustDecision: TrustDecision?,
    routeAvailable: Boolean,
    activeTransferIds: Set<String>,
  ): Boolean {
    if (transportConnected) {
      return true
    }
    if (trustDecision != null) {
      return true
    }
    if (routeAvailable) {
      return true
    }
    return activeTransferIds.isNotEmpty()
  }

  private fun clearTransferTracking(transferId: String): Unit {
    val recipientPeerId: PeerIdHex = recipientPeerIdsByTransferId.remove(transferId) ?: return
    activeTransferIdsByPeer[recipientPeerId.value]?.let { activeTransferIds ->
      activeTransferIds.remove(transferId)
      if (activeTransferIds.isEmpty()) {
        activeTransferIdsByPeer.remove(recipientPeerId.value)
      }
    }
    syncSession(peerId = recipientPeerId)
    reevaluatePowerDecision()
  }

  private fun reevaluatePowerDecision(): Unit {
    val previousDecision: PowerDecision = currentPowerDecision
    currentPowerDecision =
      powerManager.evaluate(
        connections =
          sessionRegistry.snapshot().map { record ->
            ManagedConnection(
              peerKey = PeerKey(value = record.peerId.value),
              transferStatus =
                if (record.activeTransferIds.isEmpty()) {
                  TransferStatus.IDLE
                } else {
                  TransferStatus.IN_FLIGHT
                },
              lastActivityEpochMillis = 0L,
            )
          }
      )
    if (currentPowerDecision.tier != previousDecision.tier) {
      diagnosticSink.emit(code = DiagnosticCode.POWER_TIER_CHANGED) {
        DiagnosticPayload.PowerTierChanged(
          previousTier = previousDecision.tier.name,
          currentTier = currentPowerDecision.tier.name,
        )
      }
    }
  }

  private fun withdrawRoute(route: RouteEntry, nowEpochMillis: Long): Unit {
    routingEngine.processUpdate(
      update =
        RoutingUpdate(
          destinationPeerId = route.destinationPeerId,
          nextHopPeerId = route.nextHopPeerId,
          metric = RoutingEngine.INFINITE_METRIC,
          sequenceNumber = route.sequenceNumber,
          expiresAtEpochMillis = nowEpochMillis,
        )
    )
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
