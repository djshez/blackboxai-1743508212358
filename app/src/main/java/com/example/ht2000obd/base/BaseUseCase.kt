package com.example.ht2000obd.base

import com.example.ht2000obd.utils.CoroutineUtils
import com.example.ht2000obd.utils.LogUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Base class for Use Cases (Interactors)
 *
 * @param P Parameters type.
 * @param R Result type.
 */
abstract class BaseUseCase<in P, R> {
    
    protected val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    /**
     * Execute the use case
     */
    abstract suspend operator fun invoke(params: P): Result<R>

    /**
     * Execute use case with safe error handling
     */
    protected suspend fun <T> executeSafely(
        block: suspend () -> T
    ): Result<T> = withContext(ioDispatcher) {
        try {
            Result.success(block())
        } catch (e: Exception) {
            LogUtils.e("UseCase", "${javaClass.simpleName} failed", e)
            Result.failure(e)
        }
    }

    /**
     * Handle Flow errors
     */
    protected fun <T> Flow<T>.handleErrors(): Flow<T> {
        return this
            .catch { e ->
                LogUtils.e("UseCase", "${javaClass.simpleName} flow failed", e)
                throw e
            }
            .flowOn(ioDispatcher)
    }

    /**
     * Retry operation with exponential backoff
     */
    protected suspend fun <T> retryOperation(
        times: Int = 3,
        initialDelay: Long = 100,
        maxDelay: Long = 1000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        return CoroutineUtils.retryWithBackoff(
            times = times,
            initialDelay = initialDelay,
            maxDelay = maxDelay,
            factor = factor,
            block = block
        )
    }

    /**
     * Execute operation with timeout
     */
    protected suspend fun <T> withTimeout(
        timeMillis: Long = DEFAULT_TIMEOUT,
        block: suspend () -> T
    ): T? {
        return CoroutineUtils.withCustomTimeout(
            timeMillis = timeMillis,
            block = block
        )
    }

    companion object {
        private const val DEFAULT_TIMEOUT = 30_000L // 30 seconds
    }
}

/**
 * Base class for Use Cases that don't require parameters
 */
abstract class BaseUseCaseNoParams<R> : BaseUseCase<Unit, R>() {
    
    abstract suspend fun execute(): Result<R>

    final override suspend fun invoke(params: Unit): Result<R> {
        return execute()
    }
}

/**
 * Base class for Flow Use Cases
 */
abstract class BaseFlowUseCase<in P, R> {
    
    protected val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    /**
     * Execute the use case
     */
    abstract operator fun invoke(params: P): Flow<R>

    /**
     * Handle Flow errors
     */
    protected fun <T> Flow<T>.handleErrors(): Flow<T> {
        return this
            .catch { e ->
                LogUtils.e("UseCase", "${javaClass.simpleName} flow failed", e)
                throw e
            }
            .flowOn(ioDispatcher)
    }
}

/**
 * Base class for Flow Use Cases that don't require parameters
 */
abstract class BaseFlowUseCaseNoParams<R> : BaseFlowUseCase<Unit, R>() {
    
    abstract fun execute(): Flow<R>

    final override fun invoke(params: Unit): Flow<R> {
        return execute()
    }
}

/**
 * Base class for Suspend Use Cases
 */
abstract class BaseSuspendUseCase<in P, R> {
    
    protected val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    /**
     * Execute the use case
     */
    abstract suspend operator fun invoke(params: P): R

    /**
     * Execute use case with safe error handling
     */
    protected suspend fun <T> executeSafely(
        block: suspend () -> T
    ): T = withContext(ioDispatcher) {
        try {
            block()
        } catch (e: Exception) {
            LogUtils.e("UseCase", "${javaClass.simpleName} failed", e)
            throw e
        }
    }
}

/**
 * Base class for Suspend Use Cases that don't require parameters
 */
abstract class BaseSuspendUseCaseNoParams<R> : BaseSuspendUseCase<Unit, R>() {
    
    abstract suspend fun execute(): R

    final override suspend fun invoke(params: Unit): R {
        return execute()
    }
}