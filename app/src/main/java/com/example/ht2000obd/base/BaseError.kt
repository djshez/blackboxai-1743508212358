package com.example.ht2000obd.base

import android.content.Context
import com.example.ht2000obd.R
import com.example.ht2000obd.utils.LogUtils

/**
 * Base class for error handling
 */
sealed class AppError : Exception {
    abstract val code: Int
    abstract val userMessage: String
    
    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}

/**
 * Network related errors
 */
sealed class NetworkError : AppError() {
    object NoConnection : NetworkError() {
        override val code: Int = ERROR_NO_CONNECTION
        override val userMessage: String = "No internet connection"
    }

    object Timeout : NetworkError() {
        override val code: Int = ERROR_TIMEOUT
        override val userMessage: String = "Connection timeout"
    }

    object ServerError : NetworkError() {
        override val code: Int = ERROR_SERVER
        override val userMessage: String = "Server error"
    }

    data class HttpError(
        override val code: Int,
        override val userMessage: String
    ) : NetworkError()
}

/**
 * Database related errors
 */
sealed class DatabaseError : AppError() {
    object InvalidData : DatabaseError() {
        override val code: Int = ERROR_INVALID_DATA
        override val userMessage: String = "Invalid data"
    }

    object NotFound : DatabaseError() {
        override val code: Int = ERROR_NOT_FOUND
        override val userMessage: String = "Data not found"
    }

    data class OperationFailed(
        override val userMessage: String,
        override val code: Int = ERROR_OPERATION_FAILED
    ) : DatabaseError()
}

/**
 * Bluetooth related errors
 */
sealed class BluetoothError : AppError() {
    object NotEnabled : BluetoothError() {
        override val code: Int = ERROR_BLUETOOTH_NOT_ENABLED
        override val userMessage: String = "Bluetooth is not enabled"
    }

    object NotSupported : BluetoothError() {
        override val code: Int = ERROR_BLUETOOTH_NOT_SUPPORTED
        override val userMessage: String = "Bluetooth is not supported"
    }

    object ConnectionFailed : BluetoothError() {
        override val code: Int = ERROR_BLUETOOTH_CONNECTION_FAILED
        override val userMessage: String = "Failed to connect to device"
    }

    object DeviceNotFound : BluetoothError() {
        override val code: Int = ERROR_BLUETOOTH_DEVICE_NOT_FOUND
        override val userMessage: String = "Device not found"
    }
}

/**
 * Permission related errors
 */
sealed class PermissionError : AppError() {
    data class PermissionDenied(
        val permission: String,
        override val code: Int = ERROR_PERMISSION_DENIED,
        override val userMessage: String = "Permission denied"
    ) : PermissionError()

    data class PermissionPermanentlyDenied(
        val permission: String,
        override val code: Int = ERROR_PERMISSION_PERMANENTLY_DENIED,
        override val userMessage: String = "Permission permanently denied"
    ) : PermissionError()
}

/**
 * Validation related errors
 */
sealed class ValidationError : AppError() {
    data class InvalidInput(
        val field: String,
        override val userMessage: String,
        override val code: Int = ERROR_INVALID_INPUT
    ) : ValidationError()

    data class RequiredField(
        val field: String,
        override val userMessage: String = "This field is required",
        override val code: Int = ERROR_REQUIRED_FIELD
    ) : ValidationError()
}

/**
 * Error handler interface
 */
interface ErrorHandler {
    fun handleError(error: AppError)
    fun getErrorMessage(error: AppError): String
}

/**
 * Base error handler implementation
 */
abstract class BaseErrorHandler(
    private val context: Context
) : ErrorHandler {

    override fun handleError(error: AppError) {
        LogUtils.e("Error", "Handling error: ${error.code}", error)
        
        // Log the error
        when (error) {
            is NetworkError -> handleNetworkError(error)
            is DatabaseError -> handleDatabaseError(error)
            is BluetoothError -> handleBluetoothError(error)
            is PermissionError -> handlePermissionError(error)
            is ValidationError -> handleValidationError(error)
        }

        // Show error to user
        showError(getErrorMessage(error))
    }

    override fun getErrorMessage(error: AppError): String {
        return try {
            when (error) {
                is NetworkError -> getNetworkErrorMessage(error)
                is DatabaseError -> getDatabaseErrorMessage(error)
                is BluetoothError -> getBluetoothErrorMessage(error)
                is PermissionError -> getPermissionErrorMessage(error)
                is ValidationError -> getValidationErrorMessage(error)
            }
        } catch (e: Exception) {
            LogUtils.e("Error", "Error getting error message", e)
            context.getString(R.string.error_unknown)
        }
    }

    protected abstract fun showError(message: String)

    private fun handleNetworkError(error: NetworkError) {
        when (error) {
            is NetworkError.NoConnection -> handleNoConnection()
            is NetworkError.Timeout -> handleTimeout()
            is NetworkError.ServerError -> handleServerError()
            is NetworkError.HttpError -> handleHttpError(error)
        }
    }

    private fun handleDatabaseError(error: DatabaseError) {
        when (error) {
            is DatabaseError.InvalidData -> handleInvalidData()
            is DatabaseError.NotFound -> handleDataNotFound()
            is DatabaseError.OperationFailed -> handleOperationFailed(error)
        }
    }

    private fun handleBluetoothError(error: BluetoothError) {
        when (error) {
            is BluetoothError.NotEnabled -> handleBluetoothNotEnabled()
            is BluetoothError.NotSupported -> handleBluetoothNotSupported()
            is BluetoothError.ConnectionFailed -> handleBluetoothConnectionFailed()
            is BluetoothError.DeviceNotFound -> handleBluetoothDeviceNotFound()
        }
    }

    private fun handlePermissionError(error: PermissionError) {
        when (error) {
            is PermissionError.PermissionDenied -> handlePermissionDenied(error)
            is PermissionError.PermissionPermanentlyDenied -> handlePermissionPermanentlyDenied(error)
        }
    }

    private fun handleValidationError(error: ValidationError) {
        when (error) {
            is ValidationError.InvalidInput -> handleInvalidInput(error)
            is ValidationError.RequiredField -> handleRequiredField(error)
        }
    }

    protected open fun handleNoConnection() {}
    protected open fun handleTimeout() {}
    protected open fun handleServerError() {}
    protected open fun handleHttpError(error: NetworkError.HttpError) {}
    protected open fun handleInvalidData() {}
    protected open fun handleDataNotFound() {}
    protected open fun handleOperationFailed(error: DatabaseError.OperationFailed) {}
    protected open fun handleBluetoothNotEnabled() {}
    protected open fun handleBluetoothNotSupported() {}
    protected open fun handleBluetoothConnectionFailed() {}
    protected open fun handleBluetoothDeviceNotFound() {}
    protected open fun handlePermissionDenied(error: PermissionError.PermissionDenied) {}
    protected open fun handlePermissionPermanentlyDenied(error: PermissionError.PermissionPermanentlyDenied) {}
    protected open fun handleInvalidInput(error: ValidationError.InvalidInput) {}
    protected open fun handleRequiredField(error: ValidationError.RequiredField) {}

    private fun getNetworkErrorMessage(error: NetworkError): String {
        return when (error) {
            is NetworkError.NoConnection -> context.getString(R.string.error_no_connection)
            is NetworkError.Timeout -> context.getString(R.string.error_timeout)
            is NetworkError.ServerError -> context.getString(R.string.error_server)
            is NetworkError.HttpError -> error.userMessage
        }
    }

    private fun getDatabaseErrorMessage(error: DatabaseError): String {
        return when (error) {
            is DatabaseError.InvalidData -> context.getString(R.string.error_invalid_data)
            is DatabaseError.NotFound -> context.getString(R.string.error_not_found)
            is DatabaseError.OperationFailed -> error.userMessage
        }
    }

    private fun getBluetoothErrorMessage(error: BluetoothError): String {
        return when (error) {
            is BluetoothError.NotEnabled -> context.getString(R.string.error_bluetooth_not_enabled)
            is BluetoothError.NotSupported -> context.getString(R.string.error_bluetooth_not_supported)
            is BluetoothError.ConnectionFailed -> context.getString(R.string.error_connection_failed)
            is BluetoothError.DeviceNotFound -> context.getString(R.string.error_device_not_found)
        }
    }

    private fun getPermissionErrorMessage(error: PermissionError): String {
        return when (error) {
            is PermissionError.PermissionDenied -> error.userMessage
            is PermissionError.PermissionPermanentlyDenied -> error.userMessage
        }
    }

    private fun getValidationErrorMessage(error: ValidationError): String {
        return when (error) {
            is ValidationError.InvalidInput -> error.userMessage
            is ValidationError.RequiredField -> error.userMessage
        }
    }

    companion object {
        const val ERROR_NO_CONNECTION = 1001
        const val ERROR_TIMEOUT = 1002
        const val ERROR_SERVER = 1003
        const val ERROR_INVALID_DATA = 2001
        const val ERROR_NOT_FOUND = 2002
        const val ERROR_OPERATION_FAILED = 2003
        const val ERROR_BLUETOOTH_NOT_ENABLED = 3001
        const val ERROR_BLUETOOTH_NOT_SUPPORTED = 3002
        const val ERROR_BLUETOOTH_CONNECTION_FAILED = 3003
        const val ERROR_BLUETOOTH_DEVICE_NOT_FOUND = 3004
        const val ERROR_PERMISSION_DENIED = 4001
        const val ERROR_PERMISSION_PERMANENTLY_DENIED = 4002
        const val ERROR_INVALID_INPUT = 5001
        const val ERROR_REQUIRED_FIELD = 5002
    }
}