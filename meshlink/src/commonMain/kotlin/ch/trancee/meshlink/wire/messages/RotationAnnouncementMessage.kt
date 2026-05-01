package ch.trancee.meshlink.wire.messages

import ch.trancee.meshlink.wire.WireMessage

public data class RotationAnnouncementMessage(
    public val previousPublicKey: ByteArray,
    public val nextPublicKey: ByteArray,
    public val signature: ByteArray,
) : WireMessage
