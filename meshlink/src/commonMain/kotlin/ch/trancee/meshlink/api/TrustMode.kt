package ch.trancee.meshlink.api

/** Trust policy used when a peer presents a public key. */
public enum class TrustMode {
  /** First key wins and is pinned automatically for future checks. */
  TOFU,

  /** Only previously pinned keys are accepted. */
  STRICT,

  /** Key changes or first contact require an external approval step. */
  PROMPT,
}
