package ch.trancee.meshlink.routing

import ch.trancee.meshlink.api.DiagnosticCode
import ch.trancee.meshlink.api.DiagnosticPayload
import ch.trancee.meshlink.api.DiagnosticSink
import ch.trancee.meshlink.api.NoOpDiagnosticSink
import ch.trancee.meshlink.api.PeerIdHex

public class RouteCoordinator(private val diagnosticSink: DiagnosticSink = NoOpDiagnosticSink) {
  private val sourceRecords: MutableMap<String, SourceRecord> = mutableMapOf()
  private val pendingSeqnoRequests: MutableMap<String, Int> = mutableMapOf()

  public fun isFeasible(destinationPeerId: PeerIdHex, sequenceNumber: Int, metric: Int): Boolean {
    require(sequenceNumber >= 0) {
      "RouteCoordinator sequenceNumber must be greater than or equal to 0."
    }
    require(metric >= 0) { "RouteCoordinator metric must be greater than or equal to 0." }

    val sourceRecord: SourceRecord = sourceRecords[destinationPeerId.value] ?: return true
    return sequenceNumber > sourceRecord.sequenceNumber ||
      (sequenceNumber == sourceRecord.sequenceNumber && metric < sourceRecord.metric)
  }

  public fun recordAcceptedRoute(
    destinationPeerId: PeerIdHex,
    sequenceNumber: Int,
    metric: Int,
  ): Unit {
    require(
      isFeasible(
        destinationPeerId = destinationPeerId,
        sequenceNumber = sequenceNumber,
        metric = metric,
      )
    ) {
      "RouteCoordinator rejected infeasible route for ${destinationPeerId.value}."
    }

    sourceRecords[destinationPeerId.value] =
      SourceRecord(
        destinationPeerId = destinationPeerId,
        sequenceNumber = sequenceNumber,
        metric = metric,
      )
    pendingSeqnoRequests.remove(destinationPeerId.value)
    diagnosticSink.emit(code = DiagnosticCode.ROUTE_ADDED) {
      DiagnosticPayload.RoutingChange(destinationPeerId = destinationPeerId, metric = metric)
    }
  }

  public fun requestSequenceNumber(destinationPeerId: PeerIdHex): Int {
    val nextSequenceNumber: Int = (sourceRecords[destinationPeerId.value]?.sequenceNumber ?: 0) + 1
    pendingSeqnoRequests[destinationPeerId.value] = nextSequenceNumber
    diagnosticSink.emit(code = DiagnosticCode.ROUTE_REMOVED) {
      DiagnosticPayload.RoutingChange(
        destinationPeerId = destinationPeerId,
        metric = nextSequenceNumber,
      )
    }
    return nextSequenceNumber
  }

  public fun pendingSequenceNumber(destinationPeerId: PeerIdHex): Int? {
    return pendingSeqnoRequests[destinationPeerId.value]
  }

  public fun sourceRecord(destinationPeerId: PeerIdHex): SourceRecord? {
    return sourceRecords[destinationPeerId.value]
  }

  public fun withdraw(destinationPeerId: PeerIdHex): Unit {
    if (sourceRecords.remove(destinationPeerId.value) != null) {
      pendingSeqnoRequests.remove(destinationPeerId.value)
      diagnosticSink.emit(code = DiagnosticCode.ROUTE_REMOVED) {
        DiagnosticPayload.RoutingChange(destinationPeerId = destinationPeerId, metric = 0)
      }
    }
  }
}

public data class SourceRecord(
  public val destinationPeerId: PeerIdHex,
  public val sequenceNumber: Int,
  public val metric: Int,
) {
  init {
    require(sequenceNumber >= 0) {
      "SourceRecord sequenceNumber must be greater than or equal to 0."
    }
    require(metric >= 0) { "SourceRecord metric must be greater than or equal to 0." }
  }
}
