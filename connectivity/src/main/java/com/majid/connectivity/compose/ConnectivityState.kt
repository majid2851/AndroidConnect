package com.majid.connectivity.compose

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.majid.connectivity.Connectivity
import com.majid.connectivity.NetworkStatus
import com.majid.connectivity.connectivity

/**
 * Returns a Compose [State] that reflects the current [NetworkStatus] and automatically
 * updates whenever the network changes.
 *
 * The observer is lifecycle-aware: collection is paused when the composable moves to the
 * background and resumes when it returns to the foreground.
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     val networkStatus by rememberConnectivityState()
 *
 *     if (!networkStatus.isConnected) {
 *         OfflineBanner()
 *     }
 * }
 * ```
 *
 * @param context Defaults to [LocalContext]; override only in tests or previews.
 */
@Composable
fun rememberConnectivityState(
    context: Context = LocalContext.current,
): State<NetworkStatus> {
    val connectivity = rememberConnectivity(context)
    return connectivity
        .observe()
        .collectAsStateWithLifecycle(initialValue = connectivity.currentStatus)
}

/**
 * Remembers a [Connectivity] instance for Compose usage.
 */
@Composable
fun rememberConnectivity(
    context: Context = LocalContext.current,
): Connectivity = remember(context) { connectivity(context) }
