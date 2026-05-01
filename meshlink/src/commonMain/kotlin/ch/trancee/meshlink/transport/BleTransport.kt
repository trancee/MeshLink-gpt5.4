package ch.trancee.meshlink.transport

import ch.trancee.meshlink.api.PeerIdHex
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/** Abstract BLE transport contract used by the engine. */
public interface BleTransport {
  /** Whether local advertising is currently enabled. */
  public val isAdvertising: StateFlow<Boolean>

  /** Raw inbound frame stream delivered by the transport. */
  public val receivedFrames: SharedFlow<ByteArray>

  /** Initiates a link to the remote peer. */
  public fun connect(peerId: PeerIdHex)

  /** Tears down the link to the remote peer. */
  public fun disconnect(peerId: PeerIdHex)

  /** Sends a raw frame to the remote peer. */
  public fun send(peerId: PeerIdHex, payload: ByteArray)

  /** Enables or disables local advertising. */
  public fun advertise(enabled: Boolean)
}
