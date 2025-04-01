package com.example.ht2000obd.base

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.example.ht2000obd.utils.LogUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

abstract class BaseBroadcastReceiver : BroadcastReceiver() {

    // Coroutine scope for receiver operations
    protected val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Abstract method to be implemented by child receivers
    abstract fun getIntentFilter(): IntentFilter

    /**
     * Register the receiver
     */
    fun register(context: Context) {
        try {
            context.registerReceiver(this, getIntentFilter())
            LogUtils.d("BroadcastReceiver", "${javaClass.simpleName} registered")
        } catch (e: Exception) {
            LogUtils.e("BroadcastReceiver", "Error registering receiver", e)
        }
    }

    /**
     * Unregister the receiver
     */
    fun unregister(context: Context) {
        try {
            context.unregisterReceiver(this)
            receiverScope.cancel()
            LogUtils.d("BroadcastReceiver", "${javaClass.simpleName} unregistered")
        } catch (e: Exception) {
            LogUtils.e("BroadcastReceiver", "Error unregistering receiver", e)
        }
    }

    /**
     * Safe handling of received broadcasts
     */
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            LogUtils.w("BroadcastReceiver", "Received null context or intent")
            return
        }

        try {
            handleReceive(context, intent)
        } catch (e: Exception) {
            LogUtils.e("BroadcastReceiver", "Error handling broadcast", e)
            handleError(context, intent, e)
        }
    }

    /**
     * Abstract method to handle received broadcasts
     */
    protected abstract fun handleReceive(context: Context, intent: Intent)

    /**
     * Handle errors in broadcast receiving
     */
    protected open fun handleError(context: Context, intent: Intent, error: Throwable) {
        // Override in child receivers for specific error handling
    }

    /**
     * Launch a coroutine from the receiver
     */
    protected fun launchCoroutine(block: suspend CoroutineScope.() -> Unit) {
        receiverScope.launch {
            try {
                block()
            } catch (e: Exception) {
                LogUtils.e("BroadcastReceiver", "Error in coroutine", e)
            }
        }
    }

    /**
     * Utility method to check intent action
     */
    protected fun Intent.hasAction(action: String): Boolean {
        return this.action == action
    }

    /**
     * Utility method to get boolean extra with default value
     */
    protected fun Intent.getBooleanExtraSafe(key: String, defaultValue: Boolean = false): Boolean {
        return try {
            getBooleanExtra(key, defaultValue)
        } catch (e: Exception) {
            LogUtils.e("BroadcastReceiver", "Error getting boolean extra: $key", e)
            defaultValue
        }
    }

    /**
     * Utility method to get string extra with default value
     */
    protected fun Intent.getStringExtraSafe(key: String, defaultValue: String? = null): String? {
        return try {
            getStringExtra(key) ?: defaultValue
        } catch (e: Exception) {
            LogUtils.e("BroadcastReceiver", "Error getting string extra: $key", e)
            defaultValue
        }
    }

    /**
     * Utility method to get int extra with default value
     */
    protected fun Intent.getIntExtraSafe(key: String, defaultValue: Int = 0): Int {
        return try {
            getIntExtra(key, defaultValue)
        } catch (e: Exception) {
            LogUtils.e("BroadcastReceiver", "Error getting int extra: $key", e)
            defaultValue
        }
    }

    /**
     * Utility method to get long extra with default value
     */
    protected fun Intent.getLongExtraSafe(key: String, defaultValue: Long = 0L): Long {
        return try {
            getLongExtra(key, defaultValue)
        } catch (e: Exception) {
            LogUtils.e("BroadcastReceiver", "Error getting long extra: $key", e)
            defaultValue
        }
    }

    /**
     * Utility method to get parcelable extra with default value
     */
    protected fun <T : android.os.Parcelable> Intent.getParcelableExtraSafe(
        key: String,
        clazz: Class<T>,
        defaultValue: T? = null
    ): T? {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                getParcelableExtra(key, clazz) ?: defaultValue
            } else {
                @Suppress("DEPRECATION")
                getParcelableExtra(key) ?: defaultValue
            }
        } catch (e: Exception) {
            LogUtils.e("BroadcastReceiver", "Error getting parcelable extra: $key", e)
            defaultValue
        }
    }

    companion object {
        const val EXTRA_ERROR = "extra_error"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_STATUS = "extra_status"
        const val EXTRA_TIMESTAMP = "extra_timestamp"
    }
}