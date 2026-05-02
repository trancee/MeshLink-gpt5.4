package ch.trancee.meshlink.engine

import ch.trancee.meshlink.api.PeerIdHex

/** Scope of runtime and persisted state that should be erased. */
internal sealed interface ErasureScope {
  /** Erase state associated with a single peer. */
  class Peer(internal val peerId: PeerIdHex) : ErasureScope

  /** Erase all local MeshLink state. */
  data object FactoryReset : ErasureScope
}
