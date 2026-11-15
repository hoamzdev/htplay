package com.htdev.movie.htplay.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object CheckInternet {
    fun haveNetworkConnection(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false

        // activeNetwork is the current default route used by the app.
        // Return false early when there is no active network route.
        val activeNetwork = connectivityManager.activeNetwork ?: return false

        // capabilities describe transports and validated network properties.
        // Return false early when Android cannot resolve the active network info.
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

        // Only accept transports that this app can realistically use.
        // Having a transport does not guarantee real Internet connectivity.
        val hasSupportedTransport =
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)

        // INTERNET means the network claims Internet capability.
        // VALIDATED means Android has confirmed real Internet access.
        // This filters cases such as offline Wi-Fi or unusable cellular data.
        return hasSupportedTransport &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}