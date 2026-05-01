package ch.trancee.meshlink.api

import kotlin.time.Clock
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Sink for diagnostic events emitted by MeshLink subsystems.
 *
 * Implementations are expected to be non-blocking because diagnostics are emitted on hot paths such
 * as transport, routing, and handshake processing.
 */
public interface DiagnosticSink {
  /** Bounded stream of recent diagnostic events. */
  public val diagnosticEvents: SharedFlow<DiagnosticEvent>

  /** Count of events that were dropped from the retained history due to buffer limits. */
  public val droppedEventCount: Long

  /**
   * Emits a diagnostic event.
   *
   * The payload supplier is evaluated lazily so callers can avoid constructing payload objects when
   * the sink implementation chooses a cheaper path.
   */
  public fun emit(
    code: DiagnosticCode,
    payload: () -> DiagnosticPayload = { DiagnosticPayload.None },
  )

  public companion object {
    public const val DEFAULT_BUFFER_SIZE: Int = 64

    /** Creates the default shared-flow-backed sink used by the library. */
    public fun create(
      bufferSize: Int = DEFAULT_BUFFER_SIZE,
      redactPeerIds: Boolean = false,
      clock: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    ): DiagnosticSink {
      require(bufferSize > 0) { "DiagnosticSink bufferSize must be greater than 0." }

      return SharedFlowDiagnosticSink(
        bufferSize = bufferSize,
        redactPeerIds = redactPeerIds,
        clock = clock,
      )
    }
  }
}

/** Immutable diagnostic record delivered to observers. */
public data class DiagnosticEvent(
  public val code: DiagnosticCode,
  public val severity: DiagnosticSeverity,
  public val timestampEpochMillis: Long,
  public val payload: DiagnosticPayload,
)

private class SharedFlowDiagnosticSink(
  private val bufferSize: Int,
  private val redactPeerIds: Boolean,
  private val clock: () -> Long,
) : DiagnosticSink {
  private val mutableDiagnosticEvents =
    MutableSharedFlow<DiagnosticEvent>(
      replay = bufferSize,
      extraBufferCapacity = 0,
      onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

  private var emissionCount: Long = 0

  override val diagnosticEvents: SharedFlow<DiagnosticEvent> =
    mutableDiagnosticEvents.asSharedFlow()

  override var droppedEventCount: Long = 0
    private set

  override fun emit(code: DiagnosticCode, payload: () -> DiagnosticPayload): Unit {
    // Redaction happens before the event is materialized so consumers never observe
    // the unredacted payload by accident.
    val actualPayload: DiagnosticPayload =
      payload().let { resolvedPayload ->
        if (redactPeerIds) {
          resolvedPayload.redactPeerIds()
        } else {
          resolvedPayload
        }
      }
    val event =
      DiagnosticEvent(
        code = code,
        severity = code.severity,
        timestampEpochMillis = clock(),
        payload = actualPayload,
      )

    // MutableSharedFlow with replay keeps only the latest [bufferSize] events, so we
    // track evictions explicitly for diagnostics consumers.
    if (emissionCount >= bufferSize.toLong()) {
      droppedEventCount += 1
    }
    emissionCount += 1
    mutableDiagnosticEvents.tryEmit(event)
  }
}
