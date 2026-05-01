package ch.trancee.meshlink.wire.messages

/** Logical round within the three-message Noise XX handshake. */
public enum class HandshakeRound(public val code: UByte) {
  ONE(0x01u),
  TWO(0x02u),
  THREE(0x03u);

  public companion object {
    /** Decodes the round's one-byte wire code. */
    public fun fromCode(code: UByte): HandshakeRound {
      return entries.firstOrNull { round -> round.code == code }
        ?: throw IllegalArgumentException(
          "Unknown handshake round code: 0x${code.toString(16).padStart(2, '0')}."
        )
    }
  }
}
