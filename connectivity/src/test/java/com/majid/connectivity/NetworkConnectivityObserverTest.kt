package com.majid.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NetworkConnectivityObserverTest {

    private lateinit var context: Context
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var observer: NetworkConnectivityObserver

    @Before
    fun setUp() {
        connectivityManager = mockk(relaxed = true)
        context = mockk {
            every { applicationContext } returns this
            every { getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        }
        observer = NetworkConnectivityObserver(context)
    }

    // ------------------------------------------------------------------------------------------
    // currentStatus tests
    // ------------------------------------------------------------------------------------------

    @Test
    fun `currentStatus returns Offline when no networks available`() {
        every { connectivityManager.allNetworks } returns emptyArray()
        assertEquals(NetworkStatus.Offline, observer.currentStatus)
    }

    @Test
    fun `currentStatus returns WiFi when WiFi transport present`() {
        val network = mockNetwork(transport = NetworkCapabilities.TRANSPORT_WIFI)
        every { connectivityManager.allNetworks } returns arrayOf(network.first)
        every { connectivityManager.getNetworkCapabilities(network.first) } returns network.second

        assertEquals(NetworkStatus.WiFi, observer.currentStatus)
    }

    @Test
    fun `currentStatus returns Cellular when Cellular transport present`() {
        val network = mockNetwork(transport = NetworkCapabilities.TRANSPORT_CELLULAR)
        every { connectivityManager.allNetworks } returns arrayOf(network.first)
        every { connectivityManager.getNetworkCapabilities(network.first) } returns network.second

        assertEquals(NetworkStatus.Cellular, observer.currentStatus)
    }

    @Test
    fun `currentStatus returns VPN when VPN transport present`() {
        val network = mockNetwork(transport = NetworkCapabilities.TRANSPORT_VPN)
        every { connectivityManager.allNetworks } returns arrayOf(network.first)
        every { connectivityManager.getNetworkCapabilities(network.first) } returns network.second

        assertEquals(NetworkStatus.Vpn, observer.currentStatus)
    }

    @Test
    fun `VPN takes priority over WiFi when both are active`() {
        val wifi = mockNetwork(transport = NetworkCapabilities.TRANSPORT_WIFI)
        val vpn  = mockNetwork(transport = NetworkCapabilities.TRANSPORT_VPN)
        every { connectivityManager.allNetworks } returns arrayOf(wifi.first, vpn.first)
        every { connectivityManager.getNetworkCapabilities(wifi.first) } returns wifi.second
        every { connectivityManager.getNetworkCapabilities(vpn.first) } returns vpn.second

        assertEquals(NetworkStatus.Vpn, observer.currentStatus)
    }

    @Test
    fun `isConnected is true for WiFi`() {
        assertTrue(NetworkStatus.WiFi.isConnected)
    }

    @Test
    fun `isConnected is true for Cellular`() {
        assertTrue(NetworkStatus.Cellular.isConnected)
    }

    @Test
    fun `isConnected is true for VPN`() {
        assertTrue(NetworkStatus.Vpn.isConnected)
    }

    @Test
    fun `isConnected is false for Offline`() {
        assertFalse(NetworkStatus.Offline.isConnected)
    }

    @Test
    fun `isConnected is false for Unknown`() {
        assertFalse(NetworkStatus.Unknown.isConnected)
    }

    // ------------------------------------------------------------------------------------------
    // Flow tests
    // ------------------------------------------------------------------------------------------

    @Test
    fun `observe emits initial status immediately`() = runTest {
        every { connectivityManager.allNetworks } returns emptyArray()

        observer.observe().test {
            assertEquals(NetworkStatus.Offline, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observe registers and unregisters NetworkCallback`() = runTest {
        every { connectivityManager.allNetworks } returns emptyArray()
        val callbackSlot = slot<ConnectivityManager.NetworkCallback>()
        every { connectivityManager.registerNetworkCallback(any(), capture(callbackSlot)) } returns Unit

        observer.observe().test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        verify { connectivityManager.unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>()) }
    }

    // ------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------

    private fun mockNetwork(transport: Int): Pair<Network, NetworkCapabilities> {
        val network = mockk<Network>()
        val caps = mockk<NetworkCapabilities> {
            every { hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
            every { hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns (transport == NetworkCapabilities.TRANSPORT_WIFI)
            every { hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns (transport == NetworkCapabilities.TRANSPORT_CELLULAR)
            every { hasTransport(NetworkCapabilities.TRANSPORT_VPN) } returns (transport == NetworkCapabilities.TRANSPORT_VPN)
            every { hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) } returns false
        }
        return network to caps
    }
}
