package com.example.ht2000obd.base

import android.util.Log
import com.example.ht2000obd.BuildConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Interface for logging operations
 */
interface Logger {
    fun debug(tag: String, message: String)
    fun info(tag: String, message: String)
    fun warning(tag: String, message: String)
    fun error(tag: String, message: String, throwable: Throwable? = null)
    fun verbose(tag: String, message: String)
}

/**
 * Base implementation of logger
 */
class BaseLogger(
    private val isDebugEnabled: Boolean = BuildConfig.DEBUG,
    private val logToFile: Boolean = false,
    private val logFile: File? = null
) : Logger {

    private val logQueue = ConcurrentLinkedQueue<LogEntry>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    override fun debug(tag: String, message: String) {
        if (isDebugEnabled) {
            Log.d(tag, message)
            logToFile(LogLevel.DEBUG, tag, message)
        }
    }

    override fun info(tag: String, message: String) {
        Log.i(tag, message)
        logToFile(LogLevel.INFO, tag, message)
    }

    override fun warning(tag: String, message: String) {
        Log.w(tag, message)
        logToFile(LogLevel.WARNING, tag, message)
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
            logToFile(LogLevel.ERROR, tag, "$message\n${throwable.stackTraceToString()}")
        } else {
            Log.e(tag, message)
            logToFile(LogLevel.ERROR, tag, message)
        }
    }

    override fun verbose(tag: String, message: String) {
        if (isDebugEnabled) {
            Log.v(tag, message)
            logToFile(LogLevel.VERBOSE, tag, message)
        }
    }

    private fun logToFile(level: LogLevel, tag: String, message: String) {
        if (!logToFile || logFile == null) return

        try {
            val timestamp = dateFormat.format(Date())
            val logEntry = LogEntry(timestamp, level, tag, message)
            logQueue.offer(logEntry)

            // Write logs to file in background
            Thread {
                try {
                    while (logQueue.isNotEmpty()) {
                        val entry = logQueue.poll() ?: continue
                        logFile.appendText("${entry.format()}\n")
                    }
                } catch (e: Exception) {
                    Log.e("Logger", "Error writing to log file", e)
                }
            }.start()
        } catch (e: Exception) {
            Log.e("Logger", "Error logging to file", e)
        }
    }

    /**
     * Clear log file
     */
    fun clearLogs() {
        logFile?.delete()
    }

    /**
     * Get log file contents
     */
    fun getLogs(): String {
        return logFile?.readText() ?: ""
    }

    /**
     * Get log file size
     */
    fun getLogSize(): Long {
        return logFile?.length() ?: 0
    }

    companion object {
        private const val MAX_LOG_SIZE = 5 * 1024 * 1024 // 5MB
    }
}

/**
 * Log levels
 */
enum class LogLevel {
    VERBOSE,
    DEBUG,
    INFO,
    WARNING,
    ERROR
}

/**
 * Log entry data class
 */
data class LogEntry(
    val timestamp: String,
    val level: LogLevel,
    val tag: String,
    val message: String
) {
    fun format(): String {
        return "$timestamp ${level.name} $tag: $message"
    }
}

/**
 * Logger factory
 */
object LoggerFactory {
    private var logger: Logger? = null

    fun getLogger(
        isDebugEnabled: Boolean = BuildConfig.DEBUG,
        logToFile: Boolean = false,
        logFile: File? = null
    ): Logger {
        if (logger == null) {
            logger = BaseLogger(isDebugEnabled, logToFile, logFile)
        }
        return logger!!
    }
}

/**
 * Extension functions for logging
 */
fun Any.debug(message: String) {
    LoggerFactory.getLogger().debug(this::class.java.simpleName, message)
}

fun Any.info(message: String) {
    LoggerFactory.getLogger().info(this::class.java.simpleName, message)
}

fun Any.warning(message: String) {
    LoggerFactory.getLogger().warning(this::class.java.simpleName, message)
}

fun Any.error(message: String, throwable: Throwable? = null) {
    LoggerFactory.getLogger().error(this::class.java.simpleName, message, throwable)
}

fun Any.verbose(message: String) {
    LoggerFactory.getLogger().verbose(this::class.java.simpleName, message)
}

/**
 * Crash reporting
 */
interface CrashReporter {
    fun reportCrash(throwable: Throwable, extras: Map<String, Any>? = null)
    fun log(message: String)
    fun setUserId(userId: String)
    fun setCustomKey(key: String, value: String)
}

/**
 * Base implementation of crash reporter
 */
class BaseCrashReporter : CrashReporter {
    override fun reportCrash(throwable: Throwable, extras: Map<String, Any>?) {
        // Implement crash reporting logic (e.g., Firebase Crashlytics)
        LoggerFactory.getLogger().error("CrashReporter", "Crash reported", throwable)
    }

    override fun log(message: String) {
        LoggerFactory.getLogger().info("CrashReporter", message)
    }

    override fun setUserId(userId: String) {
        // Set user ID for crash reporting
    }

    override fun setCustomKey(key: String, value: String) {
        // Set custom key for crash reporting
    }
}