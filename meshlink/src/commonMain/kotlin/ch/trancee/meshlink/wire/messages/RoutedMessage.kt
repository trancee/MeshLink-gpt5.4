package ch.trancee.meshlink.wire.messages

import ch.trancee.meshlink.wire.WireMessage

public data class RoutedMessage(
  public val hopCount: UByte,
  public val maxHops: UByte,
  public val payload: ByteArray,
) : WireMessage
