package com.majid.connectivity

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * Public facade for network observation.
 *
 * This wraps [ConnectivityObserver] so consumers can use the simple API:
 * `connectivity(context).observe()`
 */
class Connectivity internal constructor(
    private val observer: ConnectivityObserver,
) {
    /** Observe network status changes as a [Flow]. */
    fun observe(): Flow<NetworkStatus> = observer.observe()

    /** Synchronous snapshot of the current network status. */
    val currentStatus: NetworkStatus
        get() = observer.currentStatus
}

/**
 * Creates a [Connectivity] instance backed by [NetworkConnectivityObserver].
 */
fun connectivity(context: Context): Connectivity {
    return Connectivity(NetworkConnectivityObserver(context))
}
