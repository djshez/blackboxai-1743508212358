package com.example.ht2000obd.base

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.ht2000obd.utils.CoroutineUtils
import com.example.ht2000obd.utils.LogUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class BaseService : LifecycleService() {

    // Service state
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    // Binder for local service binding
    private val binder = LocalBinder()

    // Coroutine scope for service operations
    protected val serviceScope = CoroutineScope(
        SupervisorJob() + CoroutineUtils.createExceptionHandler("BaseService")
    )

    // Abstract methods to be implemented by child services
    protected abstract fun onServiceStarted()
    protected abstract fun onServiceStopped()

    override fun onCreate() {
        super.onCreate()
        LogUtils.d("Service", "${javaClass.simpleName} onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        LogUtils.d("Service", "${javaClass.simpleName} onStartCommand")
        
        handleCommand(intent)
        return Service.START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        LogUtils.d("Service", "${javaClass.simpleName} onBind")
        return binder
    }

    override fun onDestroy() {
        LogUtils.d("Service", "${javaClass.simpleName} onDestroy")
        stopService()
        serviceScope.cancel()
        super.onDestroy()
    }

    // Command handling
    protected open fun handleCommand(intent: Intent?) {
        intent?.action?.let { action ->
            when (action) {
                ACTION_START_SERVICE -> startService()
                ACTION_STOP_SERVICE -> stopService()
                else -> handleCustomCommand(action, intent)
            }
        }
    }

    // Custom command handling
    protected open fun handleCustomCommand(action: String, intent: Intent) {
        // Override in child services if needed
    }

    // Service control methods
    protected fun startService() {
        if (!isRunning.value) {
            try {
                _isRunning.value = true
                onServiceStarted()
                LogUtils.i("Service", "${javaClass.simpleName} started")
            } catch (e: Exception) {
                LogUtils.e("Service", "Error starting service", e)
                stopService()
            }
        }
    }

    protected fun stopService() {
        if (isRunning.value) {
            try {
                onServiceStopped()
                _isRunning.value = false
                LogUtils.i("Service", "${javaClass.simpleName} stopped")
            } catch (e: Exception) {
                LogUtils.e("Service", "Error stopping service", e)
            }
        }
    }

    // Coroutine utility methods
    protected fun launchInScope(
        block: suspend CoroutineScope.() -> Unit
    ): Job = serviceScope.launch {
        try {
            block()
        } catch (e: Exception) {
            LogUtils.e("Service", "Error in coroutine", e)
        }
    }

    // Binder class for local service binding
    inner class LocalBinder : Binder() {
        fun getService(): BaseService = this@BaseService
    }

    // Error handling
    protected fun handleError(error: Throwable) {
        LogUtils.e("Service", "Service error", error)
        // Override in child services for specific error handling
    }

    // Notification management
    protected fun startForeground(notificationId: Int, notification: android.app.Notification) {
        try {
            super.startForeground(notificationId, notification)
        } catch (e: Exception) {
            LogUtils.e("Service", "Error starting foreground", e)
        }
    }

    protected fun stopForeground() {
        try {
            stopForeground(true)
        } catch (e: Exception) {
            LogUtils.e("Service", "Error stopping foreground", e)
        }
    }

    // Wake lock management
    protected fun acquireWakeLock(tag: String): android.os.PowerManager.WakeLock? {
        return try {
            (getSystemService(POWER_SERVICE) as android.os.PowerManager)
                .newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, tag)
                .apply { acquire() }
        } catch (e: Exception) {
            LogUtils.e("Service", "Error acquiring wake lock", e)
            null
        }
    }

    protected fun releaseWakeLock(wakeLock: android.os.PowerManager.WakeLock?) {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
        } catch (e: Exception) {
            LogUtils.e("Service", "Error releasing wake lock", e)
        }
    }

    companion object {
        const val ACTION_START_SERVICE = "action_start_service"
        const val ACTION_STOP_SERVICE = "action_stop_service"
    }
}