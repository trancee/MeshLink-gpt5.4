package ch.trancee.meshlink.transport

/** Encodes the small BLE advertisement payload shared between peers. */
public object AdvertisementCodec {
  public const val PSEUDONYM_LENGTH_BYTES: Int = 12
  public const val APP_ID_HASH_LENGTH_BYTES: Int = Int.SIZE_BYTES
  public const val ENCODED_LENGTH_BYTES: Int = PSEUDONYM_LENGTH_BYTES + 1 + APP_ID_HASH_LENGTH_BYTES
  public const val BLE_ADVERTISEMENT_LIMIT_BYTES: Int = 31

  /** Packs a pseudonym, power tier, and application hash into service data. */
  public fun encode(
    pseudonym: ByteArray,
    powerTier: AdvertisementPowerTier,
    appIdHash: Int,
  ): ByteArray {
    require(pseudonym.size == PSEUDONYM_LENGTH_BYTES) {
      "AdvertisementCodec pseudonym must be exactly $PSEUDONYM_LENGTH_BYTES bytes."
    }

    return pseudonym + byteArrayOf(powerTier.wireCode) + appIdHash.toByteArray()
  }

  /** Parses service data back into its structured fields. */
  public fun decode(serviceData: ByteArray): AdvertisementPayload {
    require(serviceData.size == ENCODED_LENGTH_BYTES) {
      "AdvertisementCodec serviceData must be exactly $ENCODED_LENGTH_BYTES bytes."
    }

    val pseudonym: ByteArray =
      serviceData.copyOfRange(fromIndex = 0, toIndex = PSEUDONYM_LENGTH_BYTES)
    val powerTier =
      AdvertisementPowerTier.fromWireCode(wireCode = serviceData[PSEUDONYM_LENGTH_BYTES])
    val appIdHash: Int =
      serviceData
        .copyOfRange(fromIndex = PSEUDONYM_LENGTH_BYTES + 1, toIndex = ENCODED_LENGTH_BYTES)
        .toInt()
    return AdvertisementPayload(pseudonym = pseudonym, powerTier = powerTier, appIdHash = appIdHash)
  }

  /** Computes the stable 32-bit application hash advertised over BLE. */
  public fun applicationIdHash(applicationId: String): Int {
    require(applicationId.isNotBlank()) { "AdvertisementCodec applicationId must not be blank." }

    var hash: UInt = FNV1A_OFFSET_BASIS
    applicationId.encodeToByteArray().forEach { byte ->
      hash = (hash xor (byte.toUInt() and 0xFFu)) * FNV1A_PRIME
    }
    return hash.toInt()
  }

  private fun Int.toByteArray(): ByteArray {
    return byteArrayOf(
      ((this shr 24) and 0xFF).toByte(),
      ((this shr 16) and 0xFF).toByte(),
      ((this shr 8) and 0xFF).toByte(),
      (this and 0xFF).toByte(),
    )
  }

  private fun ByteArray.toInt(): Int {
    return ((this[0].toInt() and 0xFF) shl 24) or
      ((this[1].toInt() and 0xFF) shl 16) or
      ((this[2].toInt() and 0xFF) shl 8) or
      (this[3].toInt() and 0xFF)
  }

  private const val FNV1A_OFFSET_BASIS: UInt = 0x811C9DC5u
  private const val FNV1A_PRIME: UInt = 0x01000193u
}

/** Decoded advertisement service data. */
public data class AdvertisementPayload(
  public val pseudonym: ByteArray,
  public val powerTier: AdvertisementPowerTier,
  public val appIdHash: Int,
)

/** Compact power tier values advertised over BLE. */
public enum class AdvertisementPowerTier(internal val wireCode: Byte) {
  HIGH(wireCode = 0x00),
  NORMAL(wireCode = 0x01),
  LOW(wireCode = 0x02);

  public companion object {
    public fun fromWireCode(wireCode: Byte): AdvertisementPowerTier {
      return entries.firstOrNull { entry -> entry.wireCode == wireCode }
        ?: throw IllegalArgumentException(
          "AdvertisementPowerTier wireCode ${(wireCode.toInt() and 0xFF)} is not supported."
        )
    }
  }
}
