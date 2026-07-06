package com.example.ui.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun AdBanner(
    modifier: Modifier = Modifier,
    onVisibilityChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var isVisible by remember { mutableStateOf(false) }
    
    // Check if network is available
    val isNetworkConnected = remember(context) { isNetworkAvailable(context) }
    
    // Notify visibility status
    LaunchedEffect(isVisible, isNetworkConnected) {
        onVisibilityChanged(isNetworkConnected && isVisible)
    }
    
    if (isNetworkConnected) {
        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .height(if (isVisible) 50.dp else 0.dp)
                .padding(vertical = if (isVisible) 8.dp else 0.dp),
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = if (BuildConfig.AD_Ban1.isNotEmpty()) {
                        BuildConfig.AD_Ban1
                    } else {
                        "ca-app-pub-3940256099942544/6300978111" // Test Banner ID
                    }
                    adListener = object : com.google.android.gms.ads.AdListener() {
                        override fun onAdLoaded() {
                            isVisible = true
                        }
                        override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                            isVisible = false
                        }
                    }
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    } else {
        LaunchedEffect(Unit) {
            onVisibilityChanged(false)
        }
    }
}

private fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    if (connectivityManager != null) {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    return false
}
