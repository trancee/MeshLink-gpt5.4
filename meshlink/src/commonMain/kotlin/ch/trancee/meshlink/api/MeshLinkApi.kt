package ch.trancee.meshlink.api

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

public interface MeshLinkApi {
    public val state: StateFlow<MeshLinkState>

    public val peers: StateFlow<List<PeerDetail>>

    public val messages: SharedFlow<ByteArray>

    public val diagnosticEvents: SharedFlow<DiagnosticEvent>

    public fun start()

    public fun stop()

    public fun pause()

    public fun resume()

    public fun send(
        peerId: PeerIdHex,
        payload: ByteArray,
    )
}
