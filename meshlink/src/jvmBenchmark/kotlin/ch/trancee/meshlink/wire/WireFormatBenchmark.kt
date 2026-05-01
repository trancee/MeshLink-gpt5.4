package ch.trancee.meshlink.wire

import ch.trancee.meshlink.wire.messages.HelloMessage
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.Mode
import kotlinx.benchmark.OutputTimeUnit
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(kotlinx.benchmark.BenchmarkTimeUnit.MICROSECONDS)
public open class WireFormatBenchmark {
  private val message: HelloMessage =
    HelloMessage(
      peerId = byteArrayOf(0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C),
      appIdHash = 0x01020304,
    )

  @Benchmark
  public open fun encodeAndDecodeHelloMessage(): ByteArray {
    val encoded: ByteArray = WireCodec.encode(message = message)
    val decoded: WireMessage = WireCodec.decode(encoded = encoded)
    return WireCodec.encode(message = decoded)
  }
}
