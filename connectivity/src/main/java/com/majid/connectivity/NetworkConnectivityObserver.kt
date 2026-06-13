package com.majid.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Production implementation of [ConnectivityObserver] backed by [ConnectivityManager].
 *
 * Detection logic:
 * - Iterates over ALL active networks so a VPN overlay on top of WiFi/Cellular is caught.
 * - Priority: VPN > WiFi/Ethernet > Cellular > Offline
 * - Only networks that advertise [NetworkCapabilities.NET_CAPABILITY_INTERNET] are considered.
 *
 * Requires [android.Manifest.permission.ACCESS_NETWORK_STATE] (declared in the library manifest).
 *
 * @param context Any [Context]; the application context is used internally to avoid leaks.
 */
class NetworkConnectivityObserver(context: Context) : ConnectivityObserver {

    private val appContext = context.applicationContext
    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // ------------------------------------------------------------------------------------------
    // Flow
    // ------------------------------------------------------------------------------------------

    override fun observe(): Flow<NetworkStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(resolveStatus())
            }

            override fun onLost(network: Network) {
                trySend(resolveStatus())
            }

            override fun onUnavailable() {
                trySend(NetworkStatus.Offline)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                trySend(resolveStatus())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Emit the current status right away so collectors don't wait for the first change.
        trySend(resolveStatus())

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()

    // ------------------------------------------------------------------------------------------
    // Snapshot
    // ------------------------------------------------------------------------------------------

    override val currentStatus: NetworkStatus
        get() = resolveStatus()

    // ------------------------------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------------------------------

    /**
     * Determines the most relevant [NetworkStatus] by inspecting every active network.
     *
     * Checking all networks (not just the default one) correctly handles scenarios where a
     * VPN is active alongside a WiFi or Cellular connection.
     */
    @Suppress("DEPRECATION")
    private fun resolveStatus(): NetworkStatus {
        var hasWifi = false
        var hasCellular = false
        var hasVpn = false

        // allNetworks is still the only practical way to detect VPN overlays reliably.
        connectivityManager.allNetworks.forEach { network ->
            val caps = connectivityManager.getNetworkCapabilities(network) ?: return@forEach
            if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return@forEach

            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> hasVpn = true
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> hasWifi = true
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> hasCellular = true
            }
        }

        return when {
            hasVpn      -> NetworkStatus.Vpn
            hasWifi     -> NetworkStatus.WiFi
            hasCellular -> NetworkStatus.Cellular
            else        -> NetworkStatus.Offline
        }
    }
}
