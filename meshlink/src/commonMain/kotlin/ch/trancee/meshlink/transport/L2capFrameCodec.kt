package ch.trancee.meshlink.transport

import ch.trancee.meshlink.wire.ReadBuffer
import ch.trancee.meshlink.wire.WriteBuffer

/** Length-prefix framing codec for L2CAP byte streams. */
public class L2capFrameCodec(
  private val maxFrameLengthBytes: Int = DEFAULT_MAX_FRAME_LENGTH_BYTES
) {
  private var bufferedBytes: ByteArray = ByteArray(size = 0)

  init {
    require(maxFrameLengthBytes >= 0) {
      "L2capFrameCodec maxFrameLengthBytes must be greater than or equal to 0."
    }
  }

  /** Prefixes the payload with its 4-byte length. */
  public fun encode(payload: ByteArray): ByteArray {
    require(payload.size <= maxFrameLengthBytes) {
      "L2capFrameCodec frame length must be between 0 and $maxFrameLengthBytes bytes."
    }

    val writeBuffer = WriteBuffer(initialCapacity = Int.SIZE_BYTES + payload.size)
    writeBuffer.writeInt(value = payload.size)
    writeBuffer.writeBytes(value = payload)
    return writeBuffer.toByteArray()
  }

  /**
   * Appends a raw stream chunk and returns every whole frame now available.
   *
   * Partial trailing data is retained in [bufferedBytes] until more bytes arrive.
   */
  public fun append(chunk: ByteArray): List<ByteArray> {
    require(chunk.isNotEmpty() || bufferedBytes.isNotEmpty()) {
      "L2capFrameCodec append requires either a non-empty chunk or buffered frame data."
    }

    if (chunk.isNotEmpty()) {
      bufferedBytes += chunk
    }

    val frames = mutableListOf<ByteArray>()
    while (bufferedBytes.size >= Int.SIZE_BYTES) {
      val length: Int =
        ReadBuffer(source = bufferedBytes.copyOfRange(fromIndex = 0, toIndex = Int.SIZE_BYTES))
          .readInt()
      require(length in 0..maxFrameLengthBytes) {
        "L2capFrameCodec frame length must be between 0 and $maxFrameLengthBytes bytes."
      }

      val totalFrameBytes: Int = Int.SIZE_BYTES + length
      if (bufferedBytes.size < totalFrameBytes) {
        break
      }

      frames += bufferedBytes.copyOfRange(fromIndex = Int.SIZE_BYTES, toIndex = totalFrameBytes)
      bufferedBytes =
        bufferedBytes.copyOfRange(fromIndex = totalFrameBytes, toIndex = bufferedBytes.size)
    }

    return frames
  }

  public companion object {
    public const val DEFAULT_MAX_FRAME_LENGTH_BYTES: Int = 64 * 1024
  }
}
