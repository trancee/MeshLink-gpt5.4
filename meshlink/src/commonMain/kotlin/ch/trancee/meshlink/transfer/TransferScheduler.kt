package ch.trancee.meshlink.transfer

public class TransferScheduler {
  private val queuedTransfers: MutableList<ScheduledTransfer> = mutableListOf()
  private var nextSequence: Long = 0L

  public fun enqueue(transferId: String, priority: Priority): Unit {
    require(transferId.isNotBlank()) { "TransferScheduler transferId must not be blank." }

    queuedTransfers.removeAll { scheduledTransfer -> scheduledTransfer.transferId == transferId }
    queuedTransfers +=
      ScheduledTransfer(
        transferId = transferId,
        priority = priority,
        insertionSequence = nextSequence,
      )
    nextSequence += 1L
    queuedTransfers.sortWith(scheduledTransferOrdering)
  }

  public fun dequeue(): String? {
    return queuedTransfers.removeFirstOrNull()?.transferId
  }

  public fun size(): Int {
    return queuedTransfers.size
  }

  public companion object {
    private val scheduledTransferOrdering: Comparator<ScheduledTransfer> =
      compareBy<ScheduledTransfer> { scheduledTransfer ->
          when (scheduledTransfer.priority) {
            Priority.HIGH -> 0
            Priority.NORMAL -> 1
            Priority.LOW -> 2
          }
        }
        .thenBy { scheduledTransfer -> scheduledTransfer.insertionSequence }
  }
}

private data class ScheduledTransfer(
  val transferId: String,
  val priority: Priority,
  val insertionSequence: Long,
)
