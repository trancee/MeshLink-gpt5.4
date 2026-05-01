package ch.trancee.meshlink.engine

import ch.trancee.meshlink.api.MeshLinkApi
import ch.trancee.meshlink.api.MeshLinkState
import ch.trancee.meshlink.api.PeerIdHex

public class MeshNode(
    public val peerId: PeerIdHex,
    private val engine: MeshLinkApi,
) {
    public fun start(): Unit {
        engine.start()
    }

    public fun stop(): Unit {
        engine.stop()
    }

    public fun state(): MeshLinkState {
        return engine.state.value
    }
}
