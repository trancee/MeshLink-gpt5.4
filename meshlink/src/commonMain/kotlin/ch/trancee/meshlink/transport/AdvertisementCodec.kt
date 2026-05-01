package ch.trancee.meshlink.transport

public object AdvertisementCodec {
    public const val PSEUDONYM_LENGTH_BYTES: Int = 12
    public const val ENCODED_LENGTH_BYTES: Int = PSEUDONYM_LENGTH_BYTES + 1
    public const val BLE_ADVERTISEMENT_LIMIT_BYTES: Int = 31

    public fun encode(
        pseudonym: ByteArray,
        powerTier: AdvertisementPowerTier,
    ): ByteArray {
        require(pseudonym.size == PSEUDONYM_LENGTH_BYTES) {
            "AdvertisementCodec pseudonym must be exactly $PSEUDONYM_LENGTH_BYTES bytes."
        }

        return pseudonym + byteArrayOf(powerTier.wireCode)
    }

    public fun decode(serviceData: ByteArray): AdvertisementPayload {
        require(serviceData.size == ENCODED_LENGTH_BYTES) {
            "AdvertisementCodec serviceData must be exactly $ENCODED_LENGTH_BYTES bytes."
        }

        val pseudonym: ByteArray = serviceData.copyOfRange(
            fromIndex = 0,
            toIndex = PSEUDONYM_LENGTH_BYTES,
        )
        val powerTier = AdvertisementPowerTier.fromWireCode(wireCode = serviceData.last())
        return AdvertisementPayload(
            pseudonym = pseudonym,
            powerTier = powerTier,
        )
    }
}

public data class AdvertisementPayload(
    public val pseudonym: ByteArray,
    public val powerTier: AdvertisementPowerTier,
)

public enum class AdvertisementPowerTier(
    internal val wireCode: Byte,
) {
    HIGH(wireCode = 0x00),
    NORMAL(wireCode = 0x01),
    LOW(wireCode = 0x02),
    ;

    public companion object {
        public fun fromWireCode(wireCode: Byte): AdvertisementPowerTier {
            return entries.firstOrNull { entry -> entry.wireCode == wireCode }
                ?: throw IllegalArgumentException(
                    "AdvertisementPowerTier wireCode ${(wireCode.toInt() and 0xFF)} is not supported.",
                )
        }
    }
}
