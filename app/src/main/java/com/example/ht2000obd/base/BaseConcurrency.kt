package com.example.ht2000obd.base

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import com.example.ht2000obd.utils.LogUtils
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Interface for dispatcher provider
 */
interface DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val unconfined: CoroutineDispatcher
}

/**
 * Base implementation of dispatcher provider
 */
class BaseDispatcherProvider : DispatcherProvider {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
}

/**
 * Base coroutine scope
 */
abstract class BaseCoroutineScope(
    private val dispatchers: DispatcherProvider = BaseDispatcherProvider()
) {
    private val job = SupervisorJob()
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        LogUtils.e("Coroutine", "Error in coroutine", throwable)
        handleError(throwable)
    }

    protected val scope = CoroutineScope(job + dispatchers.main + exceptionHandler)
    private val activeJobs = ConcurrentHashMap<String, Job>()

    protected fun launch(
        key: String? = null,
        dispatcher: CoroutineDispatcher = dispatchers.main,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        val job = scope.launch(dispatcher) {
            try {
                block()
            } finally {
                key?.let { activeJobs.remove(it) }
            }
        }
        key?.let { activeJobs[it] = job }
        return job
    }

    protected fun <T> async(
        dispatcher: CoroutineDispatcher = dispatchers.main,
        block: suspend CoroutineScope.() -> T
    ) = scope.async(dispatcher) { block() }

    protected fun cancelJob(key: String) {
        activeJobs[key]?.cancel()
        activeJobs.remove(key)
    }

    protected fun cancelAllJobs() {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
    }

    protected abstract fun handleError(throwable: Throwable)
}

/**
 * Thread pool executor
 */
object ThreadPoolExecutor {
    private val executor = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors()
    )

    fun execute(task: Runnable) {
        executor.execute(task)
    }

    fun shutdown() {
        executor.shutdown()
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }
}

/**
 * Extension functions for coroutines
 */
suspend fun <T> withIOContext(block: suspend CoroutineScope.() -> T): T {
    return withContext(Dispatchers.IO) { block() }
}

suspend fun <T> withMainContext(block: suspend CoroutineScope.() -> T): T {
    return withContext(Dispatchers.Main) { block() }
}

suspend fun <T> withDefaultContext(block: suspend CoroutineScope.() -> T): T {
    return withContext(Dispatchers.Default) { block() }
}

suspend fun <T> withTimeoutOrNull(
    timeMillis: Long,
    block: suspend CoroutineScope.() -> T
): T? {
    return try {
        withTimeout(timeMillis) { block() }
    } catch (e: Exception) {
        LogUtils.e("Coroutine", "Timeout after $timeMillis ms", e)
        null
    }
}

fun <T> Flow<T>.flowWithIO(): Flow<T> = flowOn(Dispatchers.IO)

fun <T> Flow<T>.flowWithDefault(): Flow<T> = flowOn(Dispatchers.Default)

fun <T> Flow<T>.handleErrors(
    onError: (Throwable) -> Unit = { LogUtils.e("Flow", "Error in flow", it) }
): Flow<T> = catch { onError(it) }

/**
 * Mutex wrapper for thread safety
 */
class SafeMutex<T>(initialValue: T) {
    @Volatile
    private var value = initialValue
    private val lock = Any()

    fun get(): T {
        synchronized(lock) {
            return value
        }
    }

    fun set(newValue: T) {
        synchronized(lock) {
            value = newValue
        }
    }

    fun <R> withLock(block: (T) -> R): R {
        synchronized(lock) {
            return block(value)
        }
    }
}

/**
 * Thread-safe lazy initialization
 */
class SafeLazy<T>(private val initializer: () -> T) {
    @Volatile
    private var value: T? = null
    private val lock = Any()

    fun get(): T {
        val result = value
        if (result != null) {
            return result
        }

        synchronized(lock) {
            var resultInLock = value
            if (resultInLock == null) {
                resultInLock = initializer()
                value = resultInLock
            }
            return resultInLock
        }
    }
}

/**
 * Rate limiter for throttling operations
 */
class RateLimiter(private val timeoutMs: Long) {
    private var lastExecutionTime = 0L
    private val lock = Any()

    fun shouldProceed(): Boolean {
        synchronized(lock) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastExecutionTime >= timeoutMs) {
                lastExecutionTime = currentTime
                return true
            }
            return false
        }
    }

    fun reset() {
        synchronized(lock) {
            lastExecutionTime = 0
        }
    }
}

/**
 * Debouncer for delaying operations
 */
class Debouncer(private val delayMs: Long) {
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun debounce(action: suspend () -> Unit) {
        job?.cancel()
        job = scope.launch {
            kotlinx.coroutines.delay(delayMs)
            withContext(Dispatchers.Main) {
                action()
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}