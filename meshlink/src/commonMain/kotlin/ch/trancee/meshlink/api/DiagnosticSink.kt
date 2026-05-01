package ch.trancee.meshlink.api

import kotlin.time.Clock
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

public interface DiagnosticSink {
  public val diagnosticEvents: SharedFlow<DiagnosticEvent>

  public val droppedEventCount: Long

  public fun emit(
    code: DiagnosticCode,
    payload: () -> DiagnosticPayload = { DiagnosticPayload.None },
  )

  public companion object {
    public const val DEFAULT_BUFFER_SIZE: Int = 64

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
    // Arrange
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

    // Act
    if (emissionCount >= bufferSize.toLong()) {
      droppedEventCount += 1
    }
    emissionCount += 1
    mutableDiagnosticEvents.tryEmit(event)
  }
}
