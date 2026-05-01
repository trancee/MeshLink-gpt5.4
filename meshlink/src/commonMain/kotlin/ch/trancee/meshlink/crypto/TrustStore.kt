package ch.trancee.meshlink.crypto

import ch.trancee.meshlink.api.TrustMode

/** Minimal peer-key store implementing TOFU, STRICT, and PROMPT trust evaluation. */
public class TrustStore {
  private val pinnedKeysByPeer: MutableMap<String, ByteArray> = mutableMapOf()

  /** Evaluates a presented public key against the current trust policy. */
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
          // TOFU pins the first key we ever see for the peer and treats that as the
          // baseline for future validation.
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

  /** Pins or replaces the stored public key for a peer. */
  public fun pin(peerId: ByteArray, publicKey: ByteArray): Unit {
    pinnedKeysByPeer[peerId.toHexKey()] = publicKey.copyOf()
  }

  /** Returns a defensive copy of the pinned key, if present. */
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

/** Result of evaluating a peer key against trust policy. */
public sealed interface TrustDecision {
  /** The presented key matches a previously trusted key. */
  public data object Accepted : TrustDecision

  /** The key was newly pinned as part of TOFU onboarding. */
  public data object Pinned : TrustDecision

  /** The key must not be trusted under the current policy. */
  public data class Rejected(public val reason: String) : TrustDecision

  /** External confirmation is required before trust can be granted. */
  public data class PromptRequired(public val existingPublicKey: ByteArray?) : TrustDecision
}
