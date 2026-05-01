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

  /** Bounded diagnostic event stream for observability and debugging. */
  public val diagnosticEvents: SharedFlow<DiagnosticEvent>

  /** Transitions the runtime into the running state and enables active mesh work. */
  public fun start()

  /** Stops the runtime and releases active mesh work. */
  public fun stop()

  /** Temporarily suspends active mesh work without fully tearing down runtime state. */
  public fun pause()

  /** Resumes mesh work after a pause. */
  public fun resume()

  /**
   * Queues a payload for delivery to a specific peer.
   *
   * Implementations may stage, rate-limit, or reject the payload before it reaches the underlying
   * transport.
   */
  public fun send(peerId: PeerIdHex, payload: ByteArray)
}
