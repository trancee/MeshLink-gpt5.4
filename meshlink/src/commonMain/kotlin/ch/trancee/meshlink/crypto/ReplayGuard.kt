package ch.trancee.meshlink.crypto

public class ReplayGuard {
  private var highestSeenNonce: Long = UNINITIALIZED_HIGHEST_NONCE
  private var seenBitmap: ULong = 0uL

  public fun checkAndMark(nonce: Long): Boolean {
    if (nonce < 0L) {
      return false
    }
    if (highestSeenNonce == UNINITIALIZED_HIGHEST_NONCE) {
      highestSeenNonce = nonce
      seenBitmap = 1uL
      return true
    }
    if (nonce > highestSeenNonce) {
      val shift: Int = (nonce - highestSeenNonce).coerceAtMost(WINDOW_SIZE.toLong()).toInt()
      seenBitmap =
        if (shift >= WINDOW_SIZE) {
          0uL
        } else {
          seenBitmap shl shift
        }
      seenBitmap = seenBitmap or 1uL
      highestSeenNonce = nonce
      return true
    }

    val distance: Long = highestSeenNonce - nonce
    if (distance >= WINDOW_SIZE.toLong()) {
      return false
    }

    val mask: ULong = 1uL shl distance.toInt()
    if ((seenBitmap and mask) != 0uL) {
      return false
    }

    seenBitmap = seenBitmap or mask
    return true
  }

  public companion object {
    public const val WINDOW_SIZE: Int = 64
    private const val UNINITIALIZED_HIGHEST_NONCE: Long = -1L
  }
}
