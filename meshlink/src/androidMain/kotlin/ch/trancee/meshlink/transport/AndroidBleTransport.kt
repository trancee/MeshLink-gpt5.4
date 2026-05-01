package ch.trancee.meshlink.transport

import ch.trancee.meshlink.api.DiagnosticSink
import ch.trancee.meshlink.api.NoOpDiagnosticSink
import ch.trancee.meshlink.api.PeerIdHex
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

public class AndroidBleTransport(
    localPeerId: PeerIdHex,
    diagnosticSink: DiagnosticSink = NoOpDiagnosticSink,
) : BleTransport {
    private val delegate = VirtualMeshTransport(
        localPeerId = localPeerId,
        diagnosticSink = diagnosticSink,
    )

    override val isAdvertising: StateFlow<Boolean> = delegate.isAdvertising

    override val receivedFrames: SharedFlow<ByteArray> = delegate.receivedFrames

    public fun attachPeer(
        peerId: PeerIdHex,
        transport: VirtualMeshTransport,
    ): Unit {
        delegate.attachPeer(peerId = peerId, transport = transport)
    }

    public fun isConnected(peerId: PeerIdHex): Boolean {
        return delegate.isConnected(peerId = peerId)
    }

    override fun connect(peerId: PeerIdHex): Unit {
        delegate.connect(peerId = peerId)
    }

    override fun disconnect(peerId: PeerIdHex): Unit {
        delegate.disconnect(peerId = peerId)
    }

    override fun send(
        peerId: PeerIdHex,
        payload: ByteArray,
    ): Unit {
        delegate.send(peerId = peerId, payload = payload)
    }

    override fun advertise(enabled: Boolean): Unit {
        delegate.advertise(enabled = enabled)
    }
}
