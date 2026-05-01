package ch.trancee.meshlink.wire.messages

import ch.trancee.meshlink.wire.WireMessage

/** Single Noise handshake frame. */
public data class HandshakeMessage(
  public val round: HandshakeRound,
  public val payload: ByteArray,
) : WireMessage
