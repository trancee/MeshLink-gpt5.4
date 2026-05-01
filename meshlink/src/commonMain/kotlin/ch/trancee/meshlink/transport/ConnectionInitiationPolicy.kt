package ch.trancee.meshlink.transport

import ch.trancee.meshlink.api.PeerIdHex

public object ConnectionInitiationPolicy {
    public fun shouldLocalPeerInitiate(
        localPeerId: PeerIdHex,
        remotePeerId: PeerIdHex,
    ): Boolean {
        val normalizedLocalPeerId: String = localPeerId.value.lowercase()
        val normalizedRemotePeerId: String = remotePeerId.value.lowercase()

        if (normalizedLocalPeerId == normalizedRemotePeerId) {
            return false
        }

        return normalizedLocalPeerId < normalizedRemotePeerId
    }
}
