package com.majid.connectivity

/**
 * Represents the current network connectivity state of the device.
 *
 * Priority order when multiple transports are active: VPN > WiFi > Cellular > Offline
 */
sealed class NetworkStatus {

    /** Connected via WiFi or Ethernet. */
    data object WiFi : NetworkStatus()

    /** Connected via mobile data (LTE, 5G, 3G, etc.). */
    data object Cellular : NetworkStatus()

    /**
     * Connected through a VPN tunnel.
     * The underlying transport (WiFi or Cellular) is still active but traffic
     * is routed through the VPN.
     */
    data object Vpn : NetworkStatus()

    /** No active network connection. */
    data object Offline : NetworkStatus()

    /** Initial state before the first network check completes. */
    data object Unknown : NetworkStatus()

    /** Returns true when there is any active network connection. */
    val isConnected: Boolean
        get() = this !is Offline && this !is Unknown

    /** Human-readable label for display in UI. */
    val label: String
        get() = when (this) {
            WiFi     -> "WiFi"
            Cellular -> "Cellular"
            Vpn      -> "VPN"
            Offline  -> "Offline"
            Unknown  -> "Unknown"
        }
}
