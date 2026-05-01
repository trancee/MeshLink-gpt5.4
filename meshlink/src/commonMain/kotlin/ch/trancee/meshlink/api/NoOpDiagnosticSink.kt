package ch.trancee.meshlink.api

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

public data object NoOpDiagnosticSink : DiagnosticSink {
  private val mutableDiagnosticEvents = MutableSharedFlow<DiagnosticEvent>(replay = 0)

  override val diagnosticEvents: SharedFlow<DiagnosticEvent> =
    mutableDiagnosticEvents.asSharedFlow()

  override val droppedEventCount: Long = 0

  override fun emit(code: DiagnosticCode, payload: () -> DiagnosticPayload): Unit = Unit
}
