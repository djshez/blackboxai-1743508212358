package com.example.ht2000obd.base

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.example.ht2000obd.utils.LogUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class BaseWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    // Default dispatcher for worker operations
    protected open val dispatcher: CoroutineDispatcher = Dispatchers.IO

    /**
     * Main work execution with error handling
     */
    override suspend fun doWork(): Result {
        return try {
            LogUtils.d("Worker", "${javaClass.simpleName} started")
            
            val result = withContext(dispatcher) {
                executeWork()
            }
            
            LogUtils.d("Worker", "${javaClass.simpleName} completed")
            result
        } catch (e: CancellationException) {
            LogUtils.w("Worker", "${javaClass.simpleName} cancelled")
            Result.failure()
        } catch (e: Exception) {
            LogUtils.e("Worker", "Error executing work", e)
            handleError(e)
        }
    }

    /**
     * Abstract method to execute the actual work
     */
    protected abstract suspend fun executeWork(): Result

    /**
     * Handle worker errors
     */
    protected open fun handleError(error: Throwable): Result {
        return Result.failure(
            createOutputData(
                ERROR_KEY to error.message.orEmpty()
            )
        )
    }

    /**
     * Create output data
     */
    protected fun createOutputData(vararg pairs: Pair<String, String>): Data {
        return Data.Builder().apply {
            pairs.forEach { (key, value) ->
                putString(key, value)
            }
        }.build()
    }

    /**
     * Get input data safely
     */
    protected fun getInputString(key: String, defaultValue: String = ""): String {
        return try {
            inputData.getString(key) ?: defaultValue
        } catch (e: Exception) {
            LogUtils.e("Worker", "Error getting input string: $key", e)
            defaultValue
        }
    }

    protected fun getInputLong(key: String, defaultValue: Long = 0L): Long {
        return try {
            inputData.getLong(key, defaultValue)
        } catch (e: Exception) {
            LogUtils.e("Worker", "Error getting input long: $key", e)
            defaultValue
        }
    }

    protected fun getInputInt(key: String, defaultValue: Int = 0): Int {
        return try {
            inputData.getInt(key, defaultValue)
        } catch (e: Exception) {
            LogUtils.e("Worker", "Error getting input int: $key", e)
            defaultValue
        }
    }

    protected fun getInputBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return try {
            inputData.getBoolean(key, defaultValue)
        } catch (e: Exception) {
            LogUtils.e("Worker", "Error getting input boolean: $key", e)
            defaultValue
        }
    }

    /**
     * Create progress data
     */
    protected fun createProgressData(progress: Int): Data {
        return Data.Builder()
            .putInt(PROGRESS_KEY, progress)
            .build()
    }

    /**
     * Set progress
     */
    protected suspend fun setProgress(progress: Int) {
        try {
            setProgress(createProgressData(progress))
        } catch (e: Exception) {
            LogUtils.e("Worker", "Error setting progress", e)
        }
    }

    /**
     * Check if work should continue
     */
    protected suspend fun shouldContinue(): Boolean {
        return !isStopped
    }

    /**
     * Utility method to retry work
     */
    protected suspend fun retryWithBackoff(
        times: Int = 3,
        initialDelay: Long = 1000,
        maxDelay: Long = 10000,
        factor: Double = 2.0,
        block: suspend () -> Result
    ): Result {
        var currentDelay = initialDelay
        repeat(times - 1) {
            try {
                return block()
            } catch (e: Exception) {
                LogUtils.w("Worker", "Retry attempt ${it + 1}/$times failed", e)
                kotlinx.coroutines.delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
            }
        }
        return block() // last attempt
    }

    companion object {
        const val ERROR_KEY = "error"
        const val PROGRESS_KEY = "progress"
        const val RESULT_KEY = "result"
        const val STATUS_KEY = "status"
        
        // Common work tags
        const val TAG_SYNC = "sync"
        const val TAG_BACKUP = "backup"
        const val TAG_CLEANUP = "cleanup"
        
        // Work states
        const val STATE_PENDING = "pending"
        const val STATE_RUNNING = "running"
        const val STATE_SUCCEEDED = "succeeded"
        const val STATE_FAILED = "failed"
        const val STATE_CANCELLED = "cancelled"
    }
}