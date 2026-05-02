package ch.trancee.meshlink.transfer

import ch.trancee.meshlink.api.PeerIdHex
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.Mode
import kotlinx.benchmark.OutputTimeUnit
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(kotlinx.benchmark.BenchmarkTimeUnit.MICROSECONDS)
public open class TransferBenchmark {
  private val engine =
    TransferEngine(
      config = TransferConfig.default(),
      chunkSizePolicy = ChunkSizePolicy(gattChunkSizeBytes = 256, l2capChunkSizeBytes = 512),
    )

  @Benchmark
  public open fun processChunks(): Int {
    engine.startTransfer(
      transferId = "benchmark-transfer",
      recipientPeerId = PeerIdHex(value = "00112233"),
      priority = Priority.NORMAL,
      payload = ByteArray(size = 1024) { index -> index.toByte() },
      preferL2cap = true,
      nowEpochMillis = 0L,
    )
    val chunks = engine.nextChunks(transferId = "benchmark-transfer")
    chunks.forEachIndexed { index, _ ->
      engine.acknowledge(
        transferId = "benchmark-transfer",
        chunkIndex = index,
        nowEpochMillis = index.toLong() + 1L,
      )
    }
    return chunks.sumOf { chunk -> chunk.payload.size }
  }

  @Benchmark
  public open fun sparseAcksProduceRetryWindow(): Int {
    val engine =
      TransferEngine(
        config = TransferConfig(timeoutMillis = 100L, retransmitLimit = 2, windowSize = 4),
        chunkSizePolicy = ChunkSizePolicy(gattChunkSizeBytes = 1, l2capChunkSizeBytes = 1),
      )
    engine.startTransfer(
      transferId = "retry-transfer",
      recipientPeerId = PeerIdHex(value = "00112233"),
      priority = Priority.NORMAL,
      payload = byteArrayOf(0x01, 0x02, 0x03, 0x04),
      preferL2cap = false,
      nowEpochMillis = 0L,
    )
    engine.acknowledge(transferId = "retry-transfer", chunkIndex = 0, nowEpochMillis = 10L)
    engine.acknowledge(transferId = "retry-transfer", chunkIndex = 2, nowEpochMillis = 20L)
    return engine.nextChunks(transferId = "retry-transfer").sumOf { chunk -> chunk.chunkIndex }
  }
}
