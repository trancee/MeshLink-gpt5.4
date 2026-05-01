package ch.trancee.meshlink.routing

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
public open class RoutingBenchmark {
    private val destinationPeerId = PeerIdHex(value = "00112233")
    private val routingEngine = RoutingEngine(config = RoutingConfig.default()).apply {
        processUpdate(
            update = RoutingUpdate(
                destinationPeerId = destinationPeerId,
                nextHopPeerId = PeerIdHex(value = "44556677"),
                metric = 1,
                sequenceNumber = 1,
                expiresAtEpochMillis = 1_000L,
            ),
        )
    }

    @Benchmark
    public open fun lookupBestRoute(): PeerIdHex? {
        return routingEngine.nextHopFor(destinationPeerId = destinationPeerId)
    }
}
