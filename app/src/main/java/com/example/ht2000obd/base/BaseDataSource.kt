package com.example.ht2000obd.base

import com.example.ht2000obd.exceptions.OBDException
import com.example.ht2000obd.utils.CoroutineUtils
import com.example.ht2000obd.utils.LogUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

abstract class BaseDataSource {

    protected val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    /**
     * Safely execute a database operation
     */
    protected suspend fun <T> safeDbCall(
        errorMessage: String? = null,
        block: suspend () -> T
    ): Result<T> = withContext(ioDispatcher) {
        try {
            Result.success(block())
        } catch (e: Exception) {
            LogUtils.e("Database", errorMessage ?: "Database operation failed", e)
            Result.failure(OBDException.DatabaseError())
        }
    }

    /**
     * Safely execute a network operation
     */
    protected suspend fun <T> safeNetworkCall(
        errorMessage: String? = null,
        block: suspend () -> T
    ): Result<T> = withContext(ioDispatcher) {
        try {
            Result.success(block())
        } catch (e: Exception) {
            LogUtils.e("Network", errorMessage ?: "Network operation failed", e)
            Result.failure(e)
        }
    }

    /**
     * Safely execute a Bluetooth operation
     */
    protected suspend fun <T> safeBluetoothCall(
        errorMessage: String? = null,
        block: suspend () -> T
    ): Result<T> = withContext(ioDispatcher) {
        try {
            Result.success(block())
        } catch (e: Exception) {
            LogUtils.e("Bluetooth", errorMessage ?: "Bluetooth operation failed", e)
            Result.failure(e)
        }
    }

    /**
     * Handle Flow errors
     */
    protected fun <T> Flow<T>.handleErrors(
        errorMessage: String? = null
    ): Flow<T> {
        return this
            .catch { e ->
                LogUtils.e("Flow", errorMessage ?: "Flow operation failed", e)
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
        timeMillis: Long,
        block: suspend () -> T
    ): T? {
        return CoroutineUtils.withCustomTimeout(
            timeMillis = timeMillis,
            block = block
        )
    }

    /**
     * Execute batch operations
     */
    protected suspend fun <T> executeBatch(
        operations: List<suspend () -> T>
    ): List<Result<T>> = withContext(ioDispatcher) {
        operations.map { operation ->
            try {
                Result.success(operation())
            } catch (e: Exception) {
                LogUtils.e("Batch", "Batch operation failed", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Map entity to domain model
     */
    protected fun <T, R> T.mapToDomain(mapper: (T) -> R): R {
        return mapper(this)
    }

    /**
     * Map domain model to entity
     */
    protected fun <T, R> T.mapToEntity(mapper: (T) -> R): R {
        return mapper(this)
    }

    /**
     * Map list of entities to domain models
     */
    protected fun <T, R> List<T>.mapToDomainList(mapper: (T) -> R): List<R> {
        return map { mapper(it) }
    }

    /**
     * Map list of domain models to entities
     */
    protected fun <T, R> List<T>.mapToEntityList(mapper: (T) -> R): List<R> {
        return map { mapper(it) }
    }

    /**
     * Cache management
     */
    protected open fun clearCache() {
        // Override in child data sources if needed
    }

    /**
     * Resource cleanup
     */
    protected open fun cleanup() {
        // Override in child data sources if needed
    }

    companion object {
        private const val DEFAULT_BATCH_SIZE = 100
        private const val DEFAULT_TIMEOUT = 30_000L // 30 seconds
        private const val MAX_RETRY_ATTEMPTS = 3
    }
}