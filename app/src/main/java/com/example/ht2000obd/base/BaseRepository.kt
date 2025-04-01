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

abstract class BaseRepository {

    // Default dispatcher for IO operations
    protected val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    /**
     * Safely execute a database operation
     */
    protected suspend fun <T> safeDbCall(
        block: suspend () -> T
    ): Result<T> = withContext(ioDispatcher) {
        try {
            Result.success(block())
        } catch (e: Exception) {
            LogUtils.e("Database", "Error executing database operation", e)
            Result.failure(OBDException.DatabaseError())
        }
    }

    /**
     * Safely execute a network operation
     */
    protected suspend fun <T> safeNetworkCall(
        block: suspend () -> T
    ): Result<T> = withContext(ioDispatcher) {
        try {
            Result.success(block())
        } catch (e: Exception) {
            LogUtils.e("Network", "Error executing network operation", e)
            Result.failure(e)
        }
    }

    /**
     * Safely execute a Bluetooth operation
     */
    protected suspend fun <T> safeBluetoothCall(
        block: suspend () -> T
    ): Result<T> = withContext(ioDispatcher) {
        try {
            Result.success(block())
        } catch (e: Exception) {
            LogUtils.e("Bluetooth", "Error executing Bluetooth operation", e)
            Result.failure(e)
        }
    }

    /**
     * Extension function to handle Flow errors
     */
    protected fun <T> Flow<T>.handleErrors(): Flow<T> {
        return this
            .catch { e ->
                LogUtils.e("Flow", "Error in flow", e)
                throw e
            }
            .flowOn(ioDispatcher)
    }

    /**
     * Retry an operation with exponential backoff
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
     * Cache management
     */
    protected fun clearCache() {
        // Override in child repositories if needed
    }

    /**
     * Resource cleanup
     */
    protected open fun cleanup() {
        // Override in child repositories if needed
    }

    /**
     * Handle database transaction
     */
    protected suspend fun <T> dbTransaction(
        block: suspend () -> T
    ): Result<T> = safeDbCall {
        // Implement transaction logic here
        block()
    }

    /**
     * Handle batch operations
     */
    protected suspend fun <T> executeBatch(
        operations: List<suspend () -> T>
    ): List<Result<T>> = withContext(ioDispatcher) {
        operations.map { operation ->
            try {
                Result.success(operation())
            } catch (e: Exception) {
                LogUtils.e("Batch", "Error executing batch operation", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Utility method to map domain models to entities
     */
    protected fun <T, R> T.mapToEntity(mapper: (T) -> R): R {
        return mapper(this)
    }

    /**
     * Utility method to map entities to domain models
     */
    protected fun <T, R> T.mapFromEntity(mapper: (T) -> R): R {
        return mapper(this)
    }

    /**
     * Utility method to map list of entities
     */
    protected fun <T, R> List<T>.mapList(mapper: (T) -> R): List<R> {
        return map { mapper(it) }
    }

    companion object {
        private const val DEFAULT_TIMEOUT = 30_000L // 30 seconds
        private const val DEFAULT_BATCH_SIZE = 100
        private const val MAX_RETRY_ATTEMPTS = 3
    }
}