package dev.flare.speedtest.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dev.flare.speedtest.domain.NetworkMonitor
import dev.flare.speedtest.domain.NetworkType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Reads the active transport from [ConnectivityManager]. */
class AndroidNetworkMonitor(context: Context) : NetworkMonitor {

    private val manager = context.applicationContext.getSystemService(ConnectivityManager::class.java)

    override val networkType: Flow<NetworkType> = callbackFlow {
        trySend(currentType())
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(typeOf(manager.getNetworkCapabilities(network)))
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(typeOf(capabilities))
            }

            override fun onLost(network: Network) {
                trySend(currentType())
            }
        }
        manager.registerDefaultNetworkCallback(callback)
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    override fun currentType(): NetworkType = typeOf(manager.getNetworkCapabilities(manager.activeNetwork))

    private fun typeOf(capabilities: NetworkCapabilities?): NetworkType = when {
        capabilities == null -> NetworkType.OFFLINE
        !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> NetworkType.OFFLINE
        // A VPN rides on top of a real transport; show the real one when it is known.
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkType.VPN
        else -> NetworkType.OTHER
    }
}
