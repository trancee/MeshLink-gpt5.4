package ch.trancee.meshlink.crypto

import ch.trancee.meshlink.api.TrustMode

public class TrustStore {
  private val pinnedKeysByPeer: MutableMap<String, ByteArray> = mutableMapOf()

  public fun evaluate(
    peerId: ByteArray,
    presentedPublicKey: ByteArray,
    mode: TrustMode,
  ): TrustDecision {
    val peerKey: String = peerId.toHexKey()
    val pinnedPublicKey: ByteArray? = pinnedKeysByPeer[peerKey]

    if (pinnedPublicKey == null) {
      return when (mode) {
        TrustMode.TOFU -> {
          pinnedKeysByPeer[peerKey] = presentedPublicKey.copyOf()
          TrustDecision.Pinned
        }
        TrustMode.STRICT -> TrustDecision.Rejected(reason = "Peer is not pinned in STRICT mode.")
        TrustMode.PROMPT -> TrustDecision.PromptRequired(existingPublicKey = null)
      }
    }

    if (ConstantTimeEquals.bytes(left = pinnedPublicKey, right = presentedPublicKey)) {
      return TrustDecision.Accepted
    }

    return when (mode) {
      TrustMode.TOFU,
      TrustMode.STRICT ->
        TrustDecision.Rejected(reason = "Presented key does not match pinned key.")
      TrustMode.PROMPT -> TrustDecision.PromptRequired(existingPublicKey = pinnedPublicKey.copyOf())
    }
  }

  public fun pin(peerId: ByteArray, publicKey: ByteArray): Unit {
    pinnedKeysByPeer[peerId.toHexKey()] = publicKey.copyOf()
  }

  public fun pinnedKey(peerId: ByteArray): ByteArray? {
    return pinnedKeysByPeer[peerId.toHexKey()]?.copyOf()
  }

  private fun ByteArray.toHexKey(): String {
    return buildString(capacity = size * 2) {
      this@toHexKey.forEach { byte ->
        append((byte.toInt() and 0xFF).toString(radix = 16).padStart(length = 2, padChar = '0'))
      }
    }
  }
}

public sealed interface TrustDecision {
  public data object Accepted : TrustDecision

  public data object Pinned : TrustDecision

  public data class Rejected(public val reason: String) : TrustDecision

  public data class PromptRequired(public val existingPublicKey: ByteArray?) : TrustDecision
}
