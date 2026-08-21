package com.divafinance.server

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Asks the framework which network is Wi-Fi instead of guessing from interface order.
 *
 * Guessing does not work: on a phone with mobile data up, the cellular interface is
 * enumerated first and carriers hand out addresses like 172.23.170.4, which Java reports
 * as site-local even though it lives behind carrier NAT and is reachable from nothing.
 * The Wi-Fi address is the only one a laptop on the same network can open.
 */
class AndroidLocalAddressResolver(private val context: Context) : LocalAddressResolver {

    override fun lanAddress(): String? = wifiAddress() ?: scannedAddress()

    private fun wifiAddress(): String? = runCatching {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return null
        @Suppress("DEPRECATION") // No synchronous replacement; the callback API is async.
        val wifi = manager.allNetworks.firstOrNull { network ->
            manager.getNetworkCapabilities(network)
                ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        } ?: return null

        manager.getLinkProperties(wifi)
            ?.linkAddresses
            ?.map { it.address }
            ?.filterIsInstance<Inet4Address>()
            ?.firstOrNull { !it.isLoopbackAddress }
            ?.hostAddress
    }.getOrNull()

    /**
     * Fallback for when ConnectivityManager has nothing to say (Wi-Fi off, tethering,
     * missing permission). Point-to-point interfaces are excluded because that is what
     * cellular and VPN links are, and neither is reachable from the local network.
     */
    private fun scannedAddress(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces()
            .asSequence()
            .filter { it.isUp && !it.isLoopback && !it.isPointToPoint }
            .sortedBy { if (LAN_INTERFACE_PREFIXES.any(it.name::startsWith)) 0 else 1 }
            .flatMap { it.inetAddresses.asSequence() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { it.isSiteLocalAddress }
            ?.hostAddress
    }.getOrNull()

    private companion object {
        val LAN_INTERFACE_PREFIXES = listOf("wlan", "eth", "ap", "swlan")
    }
}
