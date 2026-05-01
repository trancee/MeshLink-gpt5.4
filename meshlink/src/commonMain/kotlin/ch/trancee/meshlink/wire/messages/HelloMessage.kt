package ch.trancee.meshlink.wire.messages

import ch.trancee.meshlink.wire.WireMessage

/** Initial discovery/compatibility message exchanged between peers. */
public data class HelloMessage(public val peerId: ByteArray, public val appIdHash: Int) :
  WireMessage
