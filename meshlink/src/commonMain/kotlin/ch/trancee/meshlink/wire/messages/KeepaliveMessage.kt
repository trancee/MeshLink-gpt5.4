package ch.trancee.meshlink.wire.messages

import ch.trancee.meshlink.wire.WireMessage

/** Empty message used to keep a link alive. */
public data object KeepaliveMessage : WireMessage
