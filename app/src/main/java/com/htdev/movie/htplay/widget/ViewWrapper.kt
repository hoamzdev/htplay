package com.htdev.movie.htplay.widget

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.View
import com.htdev.movie.htplay.nointernet.NoInternetActivity


fun View.onClickListener(block: () -> Unit) {
    var lastClickTime = 0L
    setOnClickListener {
        if(checkNetworkConnected(context)) {
            if (System.currentTimeMillis() - lastClickTime < 500L) {
                return@setOnClickListener
            }
            lastClickTime = System.currentTimeMillis()
            block()
        }
        else {
            val intent = Intent(context, NoInternetActivity::class.java)
            context.startActivity(intent)
        }
    }
}

fun View.visible(){
    visibility = View.VISIBLE
}

fun View.invisible(){
    visibility = View.INVISIBLE
}

fun View.gone(){
    visibility = View.GONE
}

fun checkNetworkConnected(context: Context): Boolean {
    val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    val capabilities = connectivityManager?.activeNetwork?.let {
        connectivityManager.getNetworkCapabilities(it)
    }
    return capabilities != null &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}