package com.example.ht2000obd.base

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.ht2000obd.utils.LogUtils
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Interface for network state
 */
sealed class NetworkState {
    object Available : NetworkState()
    object Unavailable : NetworkState()
}

/**
 * Interface for network connectivity
 */
interface NetworkConnectivity {
    fun observeNetworkState(): Flow<NetworkState>
    fun isNetworkAvailable(): Boolean
    fun isWifiConnected(): Boolean
    fun isCellularConnected(): Boolean
}

/**
 * Base implementation of network connectivity
 */
class BaseNetworkConnectivity(private val context: Context) : NetworkConnectivity {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun observeNetworkState(): Flow<NetworkState> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(NetworkState.Available)
            }

            override fun onLost(network: Network) {
                trySend(NetworkState.Unavailable)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Initial state
        trySend(if (isNetworkAvailable()) NetworkState.Available else NetworkState.Unavailable)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()

    override fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    override fun isWifiConnected(): Boolean {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }

    override fun isCellularConnected(): Boolean {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
    }
}

/**
 * Base API client builder
 */
class BaseApiClientBuilder {
    private var baseUrl: String = ""
    private var timeout: Long = 30
    private var isDebug: Boolean = false
    private var interceptors: MutableList<Interceptor> = mutableListOf()

    fun baseUrl(url: String) = apply { this.baseUrl = url }
    fun timeout(seconds: Long) = apply { this.timeout = seconds }
    fun debug(enabled: Boolean) = apply { this.isDebug = enabled }
    fun addInterceptor(interceptor: Interceptor) = apply { interceptors.add(interceptor) }

    fun build(): Retrofit {
        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(timeout, TimeUnit.SECONDS)
            .readTimeout(timeout, TimeUnit.SECONDS)
            .writeTimeout(timeout, TimeUnit.SECONDS)

        // Add custom interceptors
        interceptors.forEach { clientBuilder.addInterceptor(it) }

        // Add logging interceptor in debug mode
        if (isDebug) {
            val loggingInterceptor = HttpLoggingInterceptor { message ->
                LogUtils.d("OkHttp", message)
            }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            clientBuilder.addInterceptor(loggingInterceptor)
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(clientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}

/**
 * Base network interceptor
 */
abstract class BaseNetworkInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        return try {
            chain.proceed(request)
        } catch (e: Exception) {
            LogUtils.e("Network", "Error in network interceptor", e)
            throw e
        }
    }
}

/**
 * Authentication interceptor
 */
class AuthInterceptor(private val tokenProvider: () -> String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = tokenProvider()

        return if (token != null) {
            val newRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            chain.proceed(newRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }
}

/**
 * Cache interceptor
 */
class CacheInterceptor(private val networkConnectivity: NetworkConnectivity) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        
        return if (networkConnectivity.isNetworkAvailable()) {
            // Online: use network and cache response
            chain.proceed(request).newBuilder()
                .header("Cache-Control", "public, max-age=60")
                .build()
        } else {
            // Offline: use cached response
            val newRequest = request.newBuilder()
                .header("Cache-Control", "public, only-if-cached, max-stale=86400")
                .build()
            chain.proceed(newRequest)
        }
    }
}

/**
 * Error handling interceptor
 */
class ErrorInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (!response.isSuccessful) {
            LogUtils.e(
                "Network",
                "API error: ${response.code} - ${response.message}"
            )
        }

        return response
    }
}

/**
 * Network exceptions
 */
sealed class NetworkException : Exception() {
    class NoConnectivity : NetworkException()
    class Timeout : NetworkException()
    class ServerError(val code: Int, override val message: String) : NetworkException()
    class UnknownError(override val cause: Throwable?) : NetworkException()
}