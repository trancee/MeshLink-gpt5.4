package ch.trancee.meshlink.engine

import ch.trancee.meshlink.transport.AdvertisementPowerTier

/** Stable byte encoding for advertised power tiers. */
public object PowerTierCodec {
  public fun encode(powerTier: AdvertisementPowerTier): Byte {
    return when (powerTier) {
      AdvertisementPowerTier.HIGH -> 0x00
      AdvertisementPowerTier.NORMAL -> 0x01
      AdvertisementPowerTier.LOW -> 0x02
    }
  }

  public fun decode(encoded: Byte): AdvertisementPowerTier {
    return when (encoded.toInt() and 0xFF) {
      0x00 -> AdvertisementPowerTier.HIGH
      0x01 -> AdvertisementPowerTier.NORMAL
      0x02 -> AdvertisementPowerTier.LOW
      else ->
        throw IllegalArgumentException(
          "PowerTierCodec encoded value ${encoded.toInt() and 0xFF} is not supported."
        )
    }
  }
}
