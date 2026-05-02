package ch.trancee.meshlink.engine

import ch.trancee.meshlink.api.DiagnosticCode
import ch.trancee.meshlink.api.DiagnosticPayload
import ch.trancee.meshlink.api.DiagnosticSink
import ch.trancee.meshlink.api.NoOpDiagnosticSink
import ch.trancee.meshlink.api.PeerIdHex
import ch.trancee.meshlink.api.PeerState
import ch.trancee.meshlink.api.TrustMode
import ch.trancee.meshlink.crypto.TrustDecision
import ch.trancee.meshlink.crypto.TrustStore
import ch.trancee.meshlink.crypto.noise.HandshakeRole
import ch.trancee.meshlink.crypto.noise.NoiseSession
import ch.trancee.meshlink.crypto.noise.NoiseXXHandshake
import ch.trancee.meshlink.wire.messages.HandshakeMessage

/**
 * Owns active Noise XX handshakes keyed by remote peer.
 *
 * The manager centralizes lifecycle bookkeeping so callers do not have to remember to emit
 * diagnostics or clean up failed handshakes manually.
 */
public class NoiseHandshakeManager private constructor(settings: NoiseHandshakeSettings) {
  private val diagnosticSink: DiagnosticSink = settings.diagnosticSink
  private val trustStore: TrustStore = settings.trustStore
  private val trustMode: TrustMode = settings.trustMode
  private val handshakes: MutableMap<String, NoiseXXHandshake> = mutableMapOf()
  private val sessions: MutableMap<String, NoiseSession> = mutableMapOf()

  public constructor() : this(diagnosticSink = NoOpDiagnosticSink)

  public constructor(
    diagnosticSink: DiagnosticSink
  ) : this(
    settings =
      NoiseHandshakeSettings(
        diagnosticSink = diagnosticSink,
        trustStore = TrustStore(),
        trustMode = TrustMode.TOFU,
      )
  )

  internal constructor(
    diagnosticSink: DiagnosticSink = NoOpDiagnosticSink,
    trustStore: TrustStore = TrustStore(),
    trustMode: TrustMode,
  ) : this(
    settings =
      NoiseHandshakeSettings(
        diagnosticSink = diagnosticSink,
        trustStore = trustStore,
        trustMode = trustMode,
      )
  )

  /** Starts a fresh handshake and immediately produces the first outbound frame. */
  public fun beginHandshake(
    peerId: PeerIdHex,
    role: HandshakeRole,
    payload: ByteArray,
  ): HandshakeMessage {
    require(peerId.value !in handshakes) {
      "NoiseHandshakeManager already has an active handshake for ${peerId.value}."
    }

    val handshake = NoiseXXHandshake(role = role)
    handshakes[peerId.value] = handshake
    emitHandshakeStarted(peerId = peerId)
    return createOutboundMessage(peerId = peerId, payload = payload)
  }

  /**
   * Feeds an inbound handshake frame into the active state machine.
   *
   * Responders may lazily create their handshake state here when the peer initiated first contact.
   */
  public fun receiveHandshakeMessage(
    peerId: PeerIdHex,
    role: HandshakeRole,
    message: HandshakeMessage,
  ): Unit {
    val handshake: NoiseXXHandshake =
      handshakes.getOrPut(peerId.value) {
        emitHandshakeStarted(peerId = peerId)
        NoiseXXHandshake(role = role)
      }

    runCatching {
        handshake.receiveInboundMessage(message = message)
        if (handshake.isComplete()) {
          completeHandshake(peerId = peerId, handshake = handshake)
        }
      }
      .onFailure { throwable ->
        // Any protocol or trust error invalidates the conversation state, so the safest
        // recovery is to discard it and force a clean restart.
        handshakes.remove(peerId.value)
        emitHandshakeFailed(peerId = peerId, reason = throwable.toString())
        throw throwable
      }
  }

  /** Produces the next outbound handshake frame for an already active conversation. */
  public fun createOutboundMessage(peerId: PeerIdHex, payload: ByteArray): HandshakeMessage {
    val handshake: NoiseXXHandshake =
      requireNotNull(handshakes[peerId.value]) {
        "NoiseHandshakeManager has no active handshake for ${peerId.value}."
      }

    return runCatching {
        handshake.createOutboundMessage(payload = payload).also {
          if (handshake.isComplete()) {
            completeHandshake(peerId = peerId, handshake = handshake)
          }
        }
      }
      .onFailure { throwable ->
        handshakes.remove(peerId.value)
        emitHandshakeFailed(peerId = peerId, reason = throwable.toString())
        throw throwable
      }
      .getOrThrow()
  }

  /** Whether the peer currently has unfinished handshake state. */
  public fun isHandshakeActive(peerId: PeerIdHex): Boolean {
    return peerId.value in handshakes
  }

  internal fun session(peerId: PeerIdHex): NoiseSession? {
    return sessions[peerId.value]
  }

  private fun completeHandshake(peerId: PeerIdHex, handshake: NoiseXXHandshake): Unit {
    val remoteStaticPublicKey: ByteArray =
      requireNotNull(handshake.remoteStaticPublicKey()) {
        "NoiseHandshakeManager completed a handshake without a remote static key for ${peerId.value}."
      }
    val decision: TrustDecision =
      trustStore.evaluate(
        peerId = peerId.value.encodeToByteArray(),
        presentedPublicKey = remoteStaticPublicKey,
        mode = trustMode,
      )

    when (decision) {
      TrustDecision.Accepted,
      TrustDecision.Pinned -> {
        sessions[peerId.value] =
          requireNotNull(handshake.transportSession()) {
            "NoiseHandshakeManager completed a handshake without deriving a transport session for ${peerId.value}."
          }
        handshakes.remove(peerId.value)
        emitHandshakeSucceeded(peerId = peerId)
      }
      is TrustDecision.Rejected ->
        throw IllegalStateException(
          "NoiseHandshakeManager rejected peer ${peerId.value}: ${decision.reason}"
        )
      is TrustDecision.PromptRequired ->
        throw IllegalStateException(
          "NoiseHandshakeManager requires trust confirmation for peer ${peerId.value}."
        )
    }
  }

  private fun emitHandshakeStarted(peerId: PeerIdHex): Unit {
    diagnosticSink.emit(code = DiagnosticCode.HANDSHAKE_STARTED) {
      DiagnosticPayload.PeerLifecycle(peerId = peerId, state = PeerState.Connecting)
    }
  }

  private fun emitHandshakeSucceeded(peerId: PeerIdHex): Unit {
    diagnosticSink.emit(code = DiagnosticCode.HANDSHAKE_SUCCEEDED) {
      DiagnosticPayload.PeerLifecycle(peerId = peerId, state = PeerState.Connected)
    }
  }

  private fun emitHandshakeFailed(peerId: PeerIdHex, reason: String): Unit {
    diagnosticSink.emit(code = DiagnosticCode.HANDSHAKE_FAILED) {
      DiagnosticPayload.HandshakeFailure(peerId = peerId, reason = reason)
    }
  }

  private data class NoiseHandshakeSettings(
    val diagnosticSink: DiagnosticSink,
    val trustStore: TrustStore,
    val trustMode: TrustMode,
  )
}
