package com.astralx.browser.core.security

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkSecurityManager @Inject constructor(
    private val context: Context
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _networkBlocked = MutableStateFlow(false)
    val networkBlocked: StateFlow<Boolean> = _networkBlocked
    
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    
    fun blockAllNetworkTraffic() {
        _networkBlocked.value = true
        
        // Register network callback to monitor and block
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
            
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (_networkBlocked.value) {
                    try {
                        // Attempt to unbind network
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            connectivityManager.bindProcessToNetwork(null)
                        }
                        Timber.w("Network connection blocked due to kill switch")
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to block network")
                    }
                }
            }
        }
        
        connectivityManager.registerNetworkCallback(request, networkCallback!!)
    }
    
    fun unblockNetworkTraffic() {
        _networkBlocked.value = false
        networkCallback?.let {
            connectivityManager.unregisterNetworkCallback(it)
        }
        networkCallback = null
    }
    
    fun isNetworkAvailable(): Boolean {
        if (_networkBlocked.value) return false
        
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}