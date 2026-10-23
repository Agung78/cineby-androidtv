package com.cineby.tv.data.api

import android.content.Context
import com.cineby.tv.BuildConfig
import com.cineby.tv.CinebyTvApp
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val PREF_KEY_SERVER_URL = "custom_backend_url"
    private const val DEFAULT_URL = BuildConfig.BACKEND_API_URL

    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    @Volatile
    private var currentBaseUrl: String? = null

    fun getBaseUrl(): String {
        currentBaseUrl?.let { return it }
        val saved = try {
            val prefs = CinebyTvApp.appContext.getSharedPreferences("cineby_config", Context.MODE_PRIVATE)
            prefs.getString(PREF_KEY_SERVER_URL, null)
        } catch (_: Exception) {
            null
        }

        val url = if (!saved.isNullOrBlank()) {
            if (saved.endsWith("/")) saved else "$saved/"
        } else {
            if (DEFAULT_URL.endsWith("/")) DEFAULT_URL else "$DEFAULT_URL/"
        }
        currentBaseUrl = url
        return url
    }

    fun setBaseUrl(newUrl: String) {
        val formatted = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
        currentBaseUrl = formatted
        try {
            val prefs = CinebyTvApp.appContext.getSharedPreferences("cineby_config", Context.MODE_PRIVATE)
            prefs.edit().putString(PREF_KEY_SERVER_URL, formatted).apply()
        } catch (_: Exception) {}

        synchronized(this) {
            _cinebyApi = null
        }
    }

    private val discovery = CountDownLatch(1)
    @Volatile
    private var staleBase: okhttp3.HttpUrl? = null

    /** Call once at app start: keeps the saved/default URL if it answers, else scans the LAN for the backend. */
    fun discoverBackend() {
        staleBase = getBaseUrl().toHttpUrl()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!ServerDiscovery.isBackend(getBaseUrl())) {
                    ServerDiscovery.scanLan(CinebyTvApp.appContext)?.let { setBaseUrl(it) }
                }
            } catch (_: Exception) {
            } finally {
                discovery.countDown()
            }
        }
    }

    // Requests built against the pre-discovery URL wait for the scan, then get pointed at the discovered host.
    private val discoveryInterceptor = Interceptor { chain ->
        var req = chain.request()
        val stale = staleBase
        if (stale != null && req.url.host == stale.host && req.url.port == stale.port) {
            discovery.await(8, TimeUnit.SECONDS)
            val cur = getBaseUrl().toHttpUrl()
            req = req.newBuilder()
                .url(req.url.newBuilder().scheme(cur.scheme).host(cur.host).port(cur.port).build())
                .build()
        }
        chain.proceed(req)
    }

    private val headerInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val modifiedRequest = originalRequest.newBuilder()
            .header("Accept", "application/json, text/plain, */*")
            .header("User-Agent", "CinebyTV-Client/1.0 (Android TV)")
            .build()
        chain.proceed(modifiedRequest)
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(discoveryInterceptor)
            .addInterceptor(headerInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun buildRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Volatile
    private var _cinebyApi: CinebyApiService? = null
    val cinebyApi: CinebyApiService
        get() = _cinebyApi ?: synchronized(this) {
            _cinebyApi ?: buildRetrofit(getBaseUrl()).create(CinebyApiService::class.java).also { _cinebyApi = it }
        }
}
