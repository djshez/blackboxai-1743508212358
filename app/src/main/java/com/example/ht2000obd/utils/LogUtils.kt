package com.example.ht2000obd.utils

import android.util.Log
import com.example.ht2000obd.BuildConfig
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogUtils {
    private const val TAG = "HT2000OBD"
    private const val MAX_LOG_FILES = 5
    private const val MAX_LOG_SIZE = 5 * 1024 * 1024 // 5MB
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val fileNameFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    // Log levels
    private const val VERBOSE = 2
    private const val DEBUG = 3
    private const val INFO = 4
    private const val WARN = 5
    private const val ERROR = 6

    private var fileLogger: FileWriter? = null
    private var currentLogFile: File? = null

    /**
     * Initialize file logging
     */
    fun initializeFileLogging(logDir: File) {
        try {
            if (!logDir.exists()) {
                logDir.mkdirs()
            }

            // Clean old log files
            cleanOldLogs(logDir)

            // Create new log file
            val timestamp = fileNameFormat.format(Date())
            val logFile = File(logDir, "ht2000obd_$timestamp.log")
            currentLogFile = logFile
            fileLogger = FileWriter(logFile, true)

            // Log initialization
            i("LogUtils", "File logging initialized: ${logFile.absolutePath}")
        } catch (e: Exception) {
            e("LogUtils", "Failed to initialize file logging", e)
        }
    }

    /**
     * Clean old log files
     */
    private fun cleanOldLogs(logDir: File) {
        try {
            val logFiles = logDir.listFiles { file ->
                file.name.startsWith("ht2000obd_") && file.name.endsWith(".log")
            }?.sortedBy { it.lastModified() }

            logFiles?.let {
                while (it.size >= MAX_LOG_FILES) {
                    it.firstOrNull()?.delete()
                    it.drop(1)
                }
            }
        } catch (e: Exception) {
            e("LogUtils", "Failed to clean old logs", e)
        }
    }

    /**
     * Verbose logging
     */
    fun v(tag: String, message: String) {
        log(VERBOSE, tag, message)
    }

    /**
     * Debug logging
     */
    fun d(tag: String, message: String) {
        log(DEBUG, tag, message)
    }

    /**
     * Info logging
     */
    fun i(tag: String, message: String) {
        log(INFO, tag, message)
    }

    /**
     * Warning logging
     */
    fun w(tag: String, message: String) {
        log(WARN, tag, message)
    }

    /**
     * Error logging
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        log(ERROR, tag, message, throwable)
    }

    /**
     * Log OBD commands and responses
     */
    fun logOBD(command: String, response: String) {
        i("OBD", "Command: $command, Response: $response")
    }

    /**
     * Log Bluetooth events
     */
    fun logBluetooth(event: String, details: String? = null) {
        i("Bluetooth", "$event ${details ?: ""}")
    }

    /**
     * Main logging function
     */
    private fun log(level: Int, tag: String, message: String, throwable: Throwable? = null) {
        if (!BuildConfig.DEBUG && level <= DEBUG) return

        val timestamp = dateFormat.format(Date())
        val fullTag = "$TAG:$tag"
        val logMessage = "[$timestamp] $message"

        // Log to Android system log
        when (level) {
            VERBOSE -> Log.v(fullTag, logMessage)
            DEBUG -> Log.d(fullTag, logMessage)
            INFO -> Log.i(fullTag, logMessage)
            WARN -> Log.w(fullTag, logMessage)
            ERROR -> Log.e(fullTag, logMessage, throwable)
        }

        // Log to file if enabled
        logToFile(level, tag, logMessage, throwable)
    }

    /**
     * Write log to file
     */
    private fun logToFile(level: Int, tag: String, message: String, throwable: Throwable? = null) {
        fileLogger?.let { logger ->
            try {
                val levelString = when (level) {
                    VERBOSE -> "V"
                    DEBUG -> "D"
                    INFO -> "I"
                    WARN -> "W"
                    ERROR -> "E"
                    else -> "?"
                }

                synchronized(logger) {
                    logger.append("$levelString/$tag: $message\n")
                    throwable?.let {
                        logger.append("${it.stackTraceToString()}\n")
                    }
                    logger.flush()
                }

                // Check log file size
                checkLogFileSize()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write to log file", e)
            }
        }
    }

    /**
     * Check and rotate log file if needed
     */
    private fun checkLogFileSize() {
        currentLogFile?.let { file ->
            if (file.length() > MAX_LOG_SIZE) {
                // Close current log file
                fileLogger?.close()
                fileLogger = null

                // Start new log file
                file.parentFile?.let { logDir ->
                    initializeFileLogging(logDir)
                }
            }
        }
    }

    /**
     * Close file logger
     */
    fun closeFileLogger() {
        try {
            fileLogger?.close()
            fileLogger = null
            currentLogFile = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to close file logger", e)
        }
    }

    /**
     * Get current log file
     */
    fun getCurrentLogFile(): File? = currentLogFile
}