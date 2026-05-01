package ch.trancee.meshlink.transport

import ch.trancee.meshlink.api.PeerIdHex
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

public interface BleTransport {
  public val isAdvertising: StateFlow<Boolean>

  public val receivedFrames: SharedFlow<ByteArray>

  public fun connect(peerId: PeerIdHex)

  public fun disconnect(peerId: PeerIdHex)

  public fun send(peerId: PeerIdHex, payload: ByteArray)

  public fun advertise(enabled: Boolean)
}
