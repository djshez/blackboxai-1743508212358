package com.example.ht2000obd.utils

import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.CoroutineContext

object CoroutineUtils {
    private const val DEFAULT_TIMEOUT = 30_000L // 30 seconds

    /**
     * Application-wide coroutine scope with error handling
     */
    val applicationScope = CoroutineScope(
        SupervisorJob() + 
        Dispatchers.Default + 
        createExceptionHandler("applicationScope")
    )

    /**
     * Create a coroutine exception handler
     */
    fun createExceptionHandler(scopeName: String) = CoroutineExceptionHandler { _, throwable ->
        LogUtils.e("Coroutine", "Error in $scopeName", throwable)
    }

    /**
     * Extension function to launch a coroutine with error handling
     */
    fun CoroutineScope.launchWithHandler(
        dispatcher: CoroutineDispatcher = Dispatchers.Main,
        errorHandler: (Throwable) -> Unit = { LogUtils.e("Coroutine", "Error in coroutine", it) },
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return launch(dispatcher + createExceptionHandler("custom")) {
            try {
                block()
            } catch (e: Exception) {
                errorHandler(e)
            }
        }
    }

    /**
     * Extension function to launch a coroutine with timeout
     */
    suspend fun <T> withDefaultTimeout(block: suspend CoroutineScope.() -> T): T {
        return withTimeout(DEFAULT_TIMEOUT) {
            block()
        }
    }

    /**
     * Extension function to safely execute IO operations
     */
    suspend fun <T> safeIO(block: suspend CoroutineScope.() -> T): Result<T> {
        return try {
            withContext(Dispatchers.IO) {
                Result.success(block())
            }
        } catch (e: Exception) {
            LogUtils.e("Coroutine", "IO operation failed", e)
            Result.failure(e)
        }
    }

    /**
     * Extension function to collect flow with error handling
     */
    fun <T> Flow<T>.handleErrors(
        onError: (Throwable) -> Unit = { LogUtils.e("Flow", "Error in flow", it) }
    ): Flow<T> {
        return this.catch { throwable ->
            onError(throwable)
        }
    }

    /**
     * Extension function to collect flow on IO dispatcher
     */
    fun <T> Flow<T>.flowOnIO(): Flow<T> {
        return this.flowOn(Dispatchers.IO)
    }

    /**
     * Extension function for lifecycle scope to launch with error handling
     */
    fun LifecycleCoroutineScope.launchSafely(
        dispatcher: CoroutineDispatcher = Dispatchers.Main,
        errorHandler: (Throwable) -> Unit = { LogUtils.e("Lifecycle", "Error in lifecycle scope", it) },
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return launchWhenStarted {
            try {
                withContext(dispatcher) {
                    block()
                }
            } catch (e: Exception) {
                errorHandler(e)
            }
        }
    }

    /**
     * Coroutine context for Bluetooth operations
     */
    val BluetoothContext: CoroutineContext = Dispatchers.IO + SupervisorJob()

    /**
     * Coroutine context for Database operations
     */
    val DatabaseContext: CoroutineContext = Dispatchers.IO + SupervisorJob()

    /**
     * Coroutine context for File operations
     */
    val FileContext: CoroutineContext = Dispatchers.IO + SupervisorJob()

    /**
     * Extension function to retry an operation with exponential backoff
     */
    suspend fun <T> retryWithBackoff(
        times: Int = 3,
        initialDelay: Long = 100,
        maxDelay: Long = 1000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(times - 1) {
            try {
                return block()
            } catch (e: Exception) {
                LogUtils.w("Retry", "Operation failed, attempting retry ${it + 1}/$times")
            }
            kotlinx.coroutines.delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
        return block() // last attempt
    }

    /**
     * Extension function to run an operation with a timeout
     */
    suspend fun <T> withCustomTimeout(
        timeMillis: Long,
        onTimeout: () -> Unit = {},
        block: suspend CoroutineScope.() -> T
    ): T? {
        return try {
            withTimeout(timeMillis) {
                block()
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            LogUtils.w("Timeout", "Operation timed out after $timeMillis ms")
            onTimeout()
            null
        }
    }
}