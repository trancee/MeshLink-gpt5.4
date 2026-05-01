package ch.trancee.meshlink.routing

import ch.trancee.meshlink.api.DiagnosticCode
import ch.trancee.meshlink.api.DiagnosticPayload
import ch.trancee.meshlink.api.DiagnosticSink
import ch.trancee.meshlink.api.NoOpDiagnosticSink
import ch.trancee.meshlink.api.PeerIdHex

/**
 * Maintains per-destination source records used to apply Babel-style feasibility checks and
 * sequence-number recovery.
 */
public class RouteCoordinator(private val diagnosticSink: DiagnosticSink = NoOpDiagnosticSink) {
  // The latest accepted source record for each destination acts as the feasibility
  // reference used to reject stale or worse advertisements.
  private val sourceRecords: MutableMap<String, SourceRecord> = mutableMapOf()

  // Pending sequence-number requests remember that a destination needs a fresher
  // advertisement before route selection can progress again.
  private val pendingSeqnoRequests: MutableMap<String, Int> = mutableMapOf()

  /**
   * Returns true when an update can supersede the current source record.
   *
   * A route is feasible if it carries a newer sequence number or, for the same sequence number,
   * improves the metric.
   */
  public fun isFeasible(destinationPeerId: PeerIdHex, sequenceNumber: Int, metric: Int): Boolean {
    require(sequenceNumber >= 0) {
      "RouteCoordinator sequenceNumber must be greater than or equal to 0."
    }
    require(metric >= 0) { "RouteCoordinator metric must be greater than or equal to 0." }

    val sourceRecord: SourceRecord = sourceRecords[destinationPeerId.value] ?: return true
    return sequenceNumber > sourceRecord.sequenceNumber ||
      (sequenceNumber == sourceRecord.sequenceNumber && metric < sourceRecord.metric)
  }

  /**
   * Persists a feasible route as the new source record and clears any pending recovery request for
   * the destination.
   */
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

  /** Records that the destination needs a newer sequence number advertisement. */
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

  /** Returns the outstanding sequence-number request, if any. */
  public fun pendingSequenceNumber(destinationPeerId: PeerIdHex): Int? {
    return pendingSeqnoRequests[destinationPeerId.value]
  }

  /** Returns the last accepted source record for the destination. */
  public fun sourceRecord(destinationPeerId: PeerIdHex): SourceRecord? {
    return sourceRecords[destinationPeerId.value]
  }

  /** Removes all routing state for the destination. */
  public fun withdraw(destinationPeerId: PeerIdHex): Unit {
    if (sourceRecords.remove(destinationPeerId.value) != null) {
      pendingSeqnoRequests.remove(destinationPeerId.value)
      diagnosticSink.emit(code = DiagnosticCode.ROUTE_REMOVED) {
        DiagnosticPayload.RoutingChange(destinationPeerId = destinationPeerId, metric = 0)
      }
    }
  }
}

/** Feasibility reference for a destination. */
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
