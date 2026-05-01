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
    private val engine = TransferEngine(
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
}
