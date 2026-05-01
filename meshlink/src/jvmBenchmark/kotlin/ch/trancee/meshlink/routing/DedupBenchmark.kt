package ch.trancee.meshlink.routing

import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.Mode
import kotlinx.benchmark.OutputTimeUnit
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(kotlinx.benchmark.BenchmarkTimeUnit.MICROSECONDS)
public open class DedupBenchmark {
    private val dedupSet = DedupSet(maxEntries = 256, expiryMillis = 60_000L)
    private var nowEpochMillis: Long = 0L

    @Benchmark
    public open fun checkDuplicateKey(): Boolean {
        nowEpochMillis += 1L
        return dedupSet.isDuplicate(
            key = byteArrayOf(0x01, 0x02, 0x03, 0x04),
            nowEpochMillis = nowEpochMillis,
        )
    }
}
