package com.example.ht2000obd.base

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.ht2000obd.utils.LogUtils

/**
 * Interface for permission requests
 */
interface PermissionRequest {
    val permissions: Array<String>
    val rationaleMessage: String?
    val callback: (Boolean) -> Unit
}

/**
 * Data class for permission request implementation
 */
data class DefaultPermissionRequest(
    override val permissions: Array<String>,
    override val rationaleMessage: String? = null,
    override val callback: (Boolean) -> Unit
) : PermissionRequest {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DefaultPermissionRequest

        if (!permissions.contentEquals(other.permissions)) return false
        if (rationaleMessage != other.rationaleMessage) return false

        return true
    }

    override fun hashCode(): Int {
        var result = permissions.contentHashCode()
        result = 31 * result + (rationaleMessage?.hashCode() ?: 0)
        return result
    }
}

/**
 * Base class for handling permissions
 */
abstract class BasePermissionHandler(private val fragment: Fragment) {

    private var currentRequest: PermissionRequest? = null
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    init {
        setupPermissionLauncher()
    }

    private fun setupPermissionLauncher() {
        permissionLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.values.all { it }
            handlePermissionResult(allGranted)
        }
    }

    /**
     * Request permissions
     */
    fun requestPermissions(request: PermissionRequest) {
        try {
            currentRequest = request
            
            when {
                arePermissionsGranted(request.permissions) -> {
                    // All permissions are already granted
                    handlePermissionResult(true)
                }
                shouldShowRationale(request.permissions) && request.rationaleMessage != null -> {
                    // Show rationale dialog
                    showRationaleDialog(
                        message = request.rationaleMessage,
                        positiveAction = { launchPermissionRequest(request.permissions) },
                        negativeAction = { handlePermissionResult(false) }
                    )
                }
                else -> {
                    // Request permissions directly
                    launchPermissionRequest(request.permissions)
                }
            }
        } catch (e: Exception) {
            LogUtils.e("Permissions", "Error requesting permissions", e)
            handlePermissionResult(false)
        }
    }

    /**
     * Check if permissions are granted
     */
    fun arePermissionsGranted(permissions: Array<String>): Boolean {
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(
                fragment.requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Check if rationale should be shown
     */
    private fun shouldShowRationale(permissions: Array<String>): Boolean {
        return permissions.any { permission ->
            fragment.shouldShowRequestPermissionRationale(permission)
        }
    }

    /**
     * Launch permission request
     */
    private fun launchPermissionRequest(permissions: Array<String>) {
        try {
            permissionLauncher.launch(permissions)
        } catch (e: Exception) {
            LogUtils.e("Permissions", "Error launching permission request", e)
            handlePermissionResult(false)
        }
    }

    /**
     * Handle permission result
     */
    private fun handlePermissionResult(granted: Boolean) {
        currentRequest?.let { request ->
            request.callback(granted)
            currentRequest = null
        }
    }

    /**
     * Show rationale dialog
     */
    protected abstract fun showRationaleDialog(
        message: String,
        positiveAction: () -> Unit,
        negativeAction: () -> Unit
    )

    companion object {
        /**
         * Open app settings
         */
        fun openAppSettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                LogUtils.e("Permissions", "Error opening app settings", e)
            }
        }

        /**
         * Create permission request
         */
        fun createRequest(
            vararg permissions: String,
            rationaleMessage: String? = null,
            callback: (Boolean) -> Unit
        ): PermissionRequest {
            return DefaultPermissionRequest(
                permissions = permissions.toList().toTypedArray(),
                rationaleMessage = rationaleMessage,
                callback = callback
            )
        }
    }
}

/**
 * Extension function to check single permission
 */
fun Context.isPermissionGranted(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        permission
    ) == PackageManager.PERMISSION_GRANTED
}

/**
 * Extension function to check multiple permissions
 */
fun Context.arePermissionsGranted(vararg permissions: String): Boolean {
    return permissions.all { permission ->
        isPermissionGranted(permission)
    }
}

/**
 * Extension function to request permissions with rationale
 */
fun Fragment.requestPermissionsWithRationale(
    permissions: Array<String>,
    rationaleMessage: String? = null,
    callback: (Boolean) -> Unit
) {
    val request = DefaultPermissionRequest(
        permissions = permissions,
        rationaleMessage = rationaleMessage,
        callback = callback
    )
    (this as? BasePermissionHandler)?.requestPermissions(request)
}