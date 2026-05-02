package ch.trancee.meshlink.api

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Public runtime contract exposed to MeshLink consumers.
 *
 * The API is intentionally flow-based so callers can observe lifecycle changes, peer discovery,
 * inbound payload delivery, and diagnostics without polling.
 */
public interface MeshLinkApi {
  /**
   * Current engine lifecycle state.
   *
   * This flow always holds the latest known state.
   */
  public val state: StateFlow<MeshLinkState>

  /**
   * Snapshot of currently known peers.
   *
   * Implementations publish full snapshots instead of incremental diffs so UI and application
   * layers can render directly from the latest value.
   */
  public val peers: StateFlow<List<PeerDetail>>

  /**
   * Inbound application payload stream.
   *
   * Payloads are emitted as raw bytes because higher layers may apply their own serialization or
   * framing conventions on top of MeshLink transport.
   */
  public val messages: SharedFlow<ByteArray>

  /**
   * Bounded diagnostic event stream for observability and debugging.
   *
   * Consumers can collect this flow to surface lifecycle, routing, delivery, transfer, and
   * security-related events without coupling to internal engine types.
   */
  public val diagnosticEvents: SharedFlow<DiagnosticEvent>

  /**
   * Transitions the runtime into the running state and enables active mesh work.
   *
   * Implementations start the platform transport, emit lifecycle diagnostics, and begin publishing
   * discovery and messaging updates through the exposed flows.
   */
  public fun start()

  /**
   * Stops the runtime and releases active mesh work.
   *
   * This is the terminal lifecycle action for a running instance; callers should not assume that
   * paused or queued work remains resumable after stop completes.
   */
  public fun stop()

  /**
   * Temporarily suspends active mesh work without fully tearing down runtime state.
   *
   * Pause is intended for app-driven throttling such as foreground/background transitions where the
   * caller expects discovery and transport work to resume later.
   */
  public fun pause()

  /**
   * Resumes mesh work after a pause.
   *
   * Implementations re-enable the runtime path using the already-configured identity, transport,
   * and diagnostic wiring.
   */
  public fun resume()

  /**
   * Queues a payload for delivery to a specific peer.
   *
   * Implementations may stage, rate-limit, buffer, or reject the payload before it reaches the
   * underlying transport. Delivery semantics are intentionally asynchronous; callers should treat a
   * successful method return as acceptance into the runtime pipeline rather than as end-to-end peer
   * acknowledgment.
   */
  public fun send(peerId: PeerIdHex, payload: ByteArray)
}
