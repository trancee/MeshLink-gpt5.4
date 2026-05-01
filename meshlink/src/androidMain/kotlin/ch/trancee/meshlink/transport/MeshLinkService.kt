package ch.trancee.meshlink.transport

/**
 * Small service-style wrapper around [AndroidBleTransport].
 *
 * This models the shape of an Android bound service without depending on actual `Service` lifecycle
 * APIs in shared host-side tests.
 */
public class MeshLinkService(private var transport: AndroidBleTransport? = null) {
  /** Installs the active transport instance. */
  public fun installTransport(transport: AndroidBleTransport): Unit {
    this.transport = transport
  }

  public fun hasTransport(): Boolean {
    return transport != null
  }

  /** Starts advertising if a transport is installed. */
  public fun startAdvertising(): Boolean {
    val activeTransport: AndroidBleTransport = transport ?: return false
    activeTransport.advertise(enabled = true)
    return true
  }

  /** Stops advertising if a transport is installed. */
  public fun stopAdvertising(): Boolean {
    val activeTransport: AndroidBleTransport = transport ?: return false
    activeTransport.advertise(enabled = false)
    return true
  }

  public fun clearTransport(): Unit {
    transport = null
  }
}
