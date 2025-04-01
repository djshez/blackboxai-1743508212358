package com.example.ht2000obd

import android.app.Application
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import com.example.ht2000obd.data.AppDatabase
import com.example.ht2000obd.data.HistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class OBDApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())
    private lateinit var sharedPreferences: SharedPreferences

    // Database and repository instances
    private val database by lazy {
        AppDatabase.getDatabase(this)
    }
    val historyRepository by lazy {
        HistoryRepository(database.historyDao())
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        initializeApp()
    }

    private fun initializeApp() {
        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        // Apply theme based on saved preference
        val isDarkMode = sharedPreferences.getBoolean(KEY_DARK_MODE, false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        // Initialize other components as needed
        setupCrashReporting()
        setupLogging()
    }

    private fun setupCrashReporting() {
        // Initialize crash reporting (e.g., Firebase Crashlytics)
        // This would be implemented in a production app
    }

    private fun setupLogging() {
        // Initialize logging system
        // This would be implemented in a production app
    }

    fun setDarkMode(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
        AppCompatDelegate.setDefaultNightMode(
            if (enabled) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    fun isDarkModeEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_DARK_MODE, false)
    }

    fun setMetricUnits(useMetric: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_USE_METRIC, useMetric).apply()
    }

    fun useMetricUnits(): Boolean {
        return sharedPreferences.getBoolean(KEY_USE_METRIC, true)
    }

    fun setAutoConnect(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_AUTO_CONNECT, enabled).apply()
    }

    fun getAutoConnect(): Boolean {
        return sharedPreferences.getBoolean(KEY_AUTO_CONNECT, false)
    }

    fun setLastConnectedDevice(address: String?) {
        sharedPreferences.edit().putString(KEY_LAST_DEVICE, address).apply()
    }

    fun getLastConnectedDevice(): String? {
        return sharedPreferences.getString(KEY_LAST_DEVICE, null)
    }

    companion object {
        private const val PREFS_NAME = "obd_preferences"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_USE_METRIC = "use_metric"
        private const val KEY_AUTO_CONNECT = "auto_connect"
        private const val KEY_LAST_DEVICE = "last_device"

        @Volatile
        private var instance: OBDApplication? = null

        fun getInstance(): OBDApplication {
            return instance ?: synchronized(this) {
                instance ?: throw IllegalStateException("Application not initialized")
            }
        }
    }
}