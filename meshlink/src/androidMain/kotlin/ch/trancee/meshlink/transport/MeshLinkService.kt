package ch.trancee.meshlink.transport

public class MeshLinkService(
    private var transport: AndroidBleTransport? = null,
) {
    public fun installTransport(transport: AndroidBleTransport): Unit {
        this.transport = transport
    }

    public fun hasTransport(): Boolean {
        return transport != null
    }

    public fun startAdvertising(): Boolean {
        val activeTransport: AndroidBleTransport = transport ?: return false
        activeTransport.advertise(enabled = true)
        return true
    }

    public fun stopAdvertising(): Boolean {
        val activeTransport: AndroidBleTransport = transport ?: return false
        activeTransport.advertise(enabled = false)
        return true
    }

    public fun clearTransport(): Unit {
        transport = null
    }
}
