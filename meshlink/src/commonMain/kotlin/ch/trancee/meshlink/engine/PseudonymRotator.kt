package ch.trancee.meshlink.engine

import ch.trancee.meshlink.crypto.CryptoProvider
import ch.trancee.meshlink.transport.AdvertisementCodec

public class PseudonymRotator(
  private val cryptoProvider: CryptoProvider,
  private val epochDurationMillis: Long = DEFAULT_EPOCH_DURATION_MILLIS,
  private val pseudonymLengthBytes: Int = AdvertisementCodec.PSEUDONYM_LENGTH_BYTES,
  private val maxStaggerMillis: Long = DEFAULT_MAX_STAGGER_MILLIS,
) {
  init {
    require(epochDurationMillis > 0) {
      "PseudonymRotator epochDurationMillis must be greater than 0."
    }
    require(pseudonymLengthBytes in 1..MAX_HMAC_OUTPUT_LENGTH_BYTES) {
      "PseudonymRotator pseudonymLengthBytes must be between 1 and 32."
    }
    require(maxStaggerMillis >= 0) {
      "PseudonymRotator maxStaggerMillis must be greater than or equal to 0."
    }
  }

  public fun epochFor(timestampMillis: Long): Long {
    require(timestampMillis >= 0) {
      "PseudonymRotator timestampMillis must be greater than or equal to 0."
    }

    return timestampMillis / epochDurationMillis
  }

  public fun staggerMillis(nodeId: ByteArray): Long {
    require(nodeId.isNotEmpty()) { "PseudonymRotator nodeId must not be empty." }
    if (maxStaggerMillis == 0L) {
      return 0L
    }

    val digest: ByteArray = cryptoProvider.hmacSha256(key = nodeId, message = STAGGER_CONTEXT)
    val accumulator: Long =
      digest.take(n = 8).fold(initial = 0L) { value, byte ->
        (value shl 8) or (byte.toLong() and 0xFFL)
      }
    return accumulator % (maxStaggerMillis + 1)
  }

  public fun pseudonymForEpoch(identityKey: ByteArray, epoch: Long): ByteArray {
    require(identityKey.isNotEmpty()) { "PseudonymRotator identityKey must not be empty." }
    require(epoch >= 0) { "PseudonymRotator epoch must be greater than or equal to 0." }

    val epochMessage: ByteArray =
      byteArrayOf(
        ((epoch ushr 56) and 0xFF).toByte(),
        ((epoch ushr 48) and 0xFF).toByte(),
        ((epoch ushr 40) and 0xFF).toByte(),
        ((epoch ushr 32) and 0xFF).toByte(),
        ((epoch ushr 24) and 0xFF).toByte(),
        ((epoch ushr 16) and 0xFF).toByte(),
        ((epoch ushr 8) and 0xFF).toByte(),
        (epoch and 0xFF).toByte(),
      ) + PSEUDONYM_CONTEXT
    return cryptoProvider
      .hmacSha256(key = identityKey, message = epochMessage)
      .copyOf(newSize = pseudonymLengthBytes)
  }

  public fun pseudonymAt(identityKey: ByteArray, timestampMillis: Long): ByteArray {
    return pseudonymForEpoch(
      identityKey = identityKey,
      epoch = epochFor(timestampMillis = timestampMillis),
    )
  }

  public fun isValidForCurrentWindow(
    candidate: ByteArray,
    identityKey: ByteArray,
    timestampMillis: Long,
  ): Boolean {
    require(candidate.size == pseudonymLengthBytes) {
      "PseudonymRotator candidate pseudonym must be exactly $pseudonymLengthBytes bytes."
    }

    val currentEpoch: Long = epochFor(timestampMillis = timestampMillis)
    val candidateEpochs: List<Long> = buildList {
      if (currentEpoch > 0) {
        add(currentEpoch - 1)
      }
      add(currentEpoch)
      add(currentEpoch + 1)
    }

    return candidateEpochs.any { epoch ->
      pseudonymForEpoch(identityKey = identityKey, epoch = epoch).contentEquals(candidate)
    }
  }

  public companion object {
    public const val DEFAULT_EPOCH_DURATION_MILLIS: Long = 15 * 60 * 1000L
    public const val DEFAULT_MAX_STAGGER_MILLIS: Long = 30_000L
    private const val MAX_HMAC_OUTPUT_LENGTH_BYTES: Int = 32
    private val STAGGER_CONTEXT: ByteArray = "meshlink-pseudonym-stagger".encodeToByteArray()
    private val PSEUDONYM_CONTEXT: ByteArray = "meshlink-pseudonym".encodeToByteArray()
  }
}
