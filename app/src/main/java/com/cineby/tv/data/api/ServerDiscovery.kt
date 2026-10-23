package com.cineby.tv.data.api

import android.content.Context
import android.net.ConnectivityManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.Inet4Address
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/** Finds the backend on the current LAN by probing /health on every host of the device's /24. */
object ServerDiscovery {

    private const val PORT = 5376 // ponytail: fixed port; make configurable if the backend port ever changes

    private val client = OkHttpClient.Builder()
        .connectTimeout(500, TimeUnit.MILLISECONDS)
        .readTimeout(1, TimeUnit.SECONDS)
        .build()

    fun isBackend(baseUrl: String): Boolean = try {
        val req = Request.Builder().url("${baseUrl.trimEnd('/')}/health").build()
        client.newCall(req).execute().use {
            it.isSuccessful && it.body?.string()?.contains("\"status\":\"ok\"") == true
        }
    } catch (_: Exception) {
        false
    }

    suspend fun scanLan(context: Context): String? {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val ip = cm.getLinkProperties(cm.activeNetwork)?.linkAddresses
            ?.map { it.address }?.filterIsInstance<Inet4Address>()?.firstOrNull()
            ?: return null
        val b = ip.address.map { it.toInt() and 255 }
        val prefix = "${b[0]}.${b[1]}.${b[2]}" // ponytail: assumes /24, widen if a LAN is bigger

        return coroutineScope {
            val found = CompletableDeferred<String?>()
            val gate = Semaphore(64)
            val jobs = (1..254).filter { it != b[3] }.map { i ->
                launch(Dispatchers.IO) {
                    gate.withPermit {
                        val url = "http://$prefix.$i:$PORT/"
                        if (found.isActive && isBackend(url)) found.complete(url)
                    }
                }
            }
            launch { jobs.joinAll(); found.complete(null) }
            found.await().also { coroutineContext.cancelChildren() }
        }
    }
}
