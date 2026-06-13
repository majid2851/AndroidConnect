package com.majid.connectivity

import kotlinx.coroutines.flow.Flow

/**
 * Contract for observing device network connectivity changes.
 *
 * Usage:
 * ```kotlin
 * val observer: ConnectivityObserver = NetworkConnectivityObserver(context)
 *
 * // Collect as a Flow
 * observer.observe().collect { status ->
 *     when (status) {
 *         NetworkStatus.WiFi     -> // connected via WiFi
 *         NetworkStatus.Cellular -> // connected via mobile data
 *         NetworkStatus.Vpn      -> // connected via VPN
 *         NetworkStatus.Offline  -> // no connection
 *         NetworkStatus.Unknown  -> // initial/transitioning
 *     }
 * }
 *
 * // Or read the current snapshot
 * val current = observer.currentStatus
 * ```
 */
interface ConnectivityObserver {

    /**
     * Emits [NetworkStatus] whenever the network state changes.
     *
     * - Starts with the current status immediately upon collection.
     * - Emits only distinct values (no duplicate emissions for the same state).
     * - The flow completes when the enclosing coroutine scope is cancelled.
     */
    fun observe(): Flow<NetworkStatus>

    /**
     * Synchronous snapshot of the current network status.
     * Useful for one-shot checks without collecting a flow.
     */
    val currentStatus: NetworkStatus
}
