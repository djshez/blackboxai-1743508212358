package com.example.ht2000obd.base

import android.content.Context
import com.example.ht2000obd.utils.LogUtils
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Interface for configuration values
 */
interface ConfigValue<T> {
    val key: String
    val defaultValue: T
    val isEncrypted: Boolean
        get() = false
}

/**
 * Data class implementing configuration value
 */
data class DefaultConfigValue<T>(
    override val key: String,
    override val defaultValue: T,
    override val isEncrypted: Boolean = false
) : ConfigValue<T>

/**
 * Interface for configuration manager
 */
interface ConfigManager {
    fun <T> get(config: ConfigValue<T>): T
    fun <T> set(config: ConfigValue<T>, value: T)
    fun <T> observe(config: ConfigValue<T>): Flow<T>
    fun clear()
    fun clearEncrypted()
}

/**
 * Base class for configuration manager implementation
 */
abstract class BaseConfigManager(
    protected val context: Context
) : ConfigManager {

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    private val configCache = mutableMapOf<String, Any?>()
    private val configFlows = mutableMapOf<String, MutableStateFlow<Any?>>()
    private val isInitialized = AtomicBoolean(false)

    init {
        loadConfiguration()
    }

    /**
     * Load configuration from storage
     */
    private fun loadConfiguration() {
        try {
            if (!isInitialized.getAndSet(true)) {
                val configFile = getConfigFile()
                if (configFile.exists()) {
                    val json = configFile.readText()
                    val type = object : TypeToken<Map<String, Any?>>() {}.type
                    val config: Map<String, Any?> = gson.fromJson(json, type)
                    configCache.putAll(config)
                }
            }
        } catch (e: Exception) {
            LogUtils.e("Config", "Error loading configuration", e)
        }
    }

    /**
     * Save configuration to storage
     */
    protected fun saveConfiguration() {
        try {
            val configFile = getConfigFile()
            val json = gson.toJson(configCache)
            configFile.writeText(json)
        } catch (e: Exception) {
            LogUtils.e("Config", "Error saving configuration", e)
        }
    }

    /**
     * Get configuration file
     */
    protected fun getConfigFile(): File {
        return File(context.filesDir, CONFIG_FILE_NAME)
    }

    override fun <T> get(config: ConfigValue<T>): T {
        return try {
            @Suppress("UNCHECKED_CAST")
            configCache.getOrDefault(config.key, config.defaultValue) as T
        } catch (e: Exception) {
            LogUtils.e("Config", "Error getting config value: ${config.key}", e)
            config.defaultValue
        }
    }

    override fun <T> set(config: ConfigValue<T>, value: T) {
        try {
            configCache[config.key] = value
            configFlows[config.key]?.tryEmit(value)
            saveConfiguration()
        } catch (e: Exception) {
            LogUtils.e("Config", "Error setting config value: ${config.key}", e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> observe(config: ConfigValue<T>): Flow<T> {
        return try {
            configFlows.getOrPut(config.key) {
                MutableStateFlow(get(config))
            }.asStateFlow() as Flow<T>
        } catch (e: Exception) {
            LogUtils.e("Config", "Error observing config value: ${config.key}", e)
            MutableStateFlow(config.defaultValue).asStateFlow()
        }
    }

    override fun clear() {
        try {
            configCache.clear()
            configFlows.clear()
            getConfigFile().delete()
        } catch (e: Exception) {
            LogUtils.e("Config", "Error clearing configuration", e)
        }
    }

    override fun clearEncrypted() {
        try {
            val encryptedKeys = configCache.keys.filter { key ->
                configCache[key] is ConfigValue<*> && (configCache[key] as ConfigValue<*>).isEncrypted
            }
            encryptedKeys.forEach { key ->
                configCache.remove(key)
                configFlows.remove(key)
            }
            saveConfiguration()
        } catch (e: Exception) {
            LogUtils.e("Config", "Error clearing encrypted configuration", e)
        }
    }

    companion object {
        private const val CONFIG_FILE_NAME = "app_config.json"
    }
}

/**
 * Common configuration values
 */
object CommonConfig {
    val THEME = DefaultConfigValue("theme", "system")
    val LANGUAGE = DefaultConfigValue("language", "en")
    val NOTIFICATIONS_ENABLED = DefaultConfigValue("notifications_enabled", true)
    val LAST_SYNC = DefaultConfigValue("last_sync", 0L)
    val AUTH_TOKEN = DefaultConfigValue("auth_token", "", isEncrypted = true)
    val REFRESH_TOKEN = DefaultConfigValue("refresh_token", "", isEncrypted = true)
    val USER_ID = DefaultConfigValue("user_id", "")
    val APP_VERSION = DefaultConfigValue("app_version", "1.0.0")
    val FIRST_LAUNCH = DefaultConfigValue("first_launch", true)
    val ONBOARDING_COMPLETED = DefaultConfigValue("onboarding_completed", false)
}

/**
 * Extension function to get typed configuration value
 */
inline fun <reified T> ConfigManager.getTyped(key: String, defaultValue: T): T {
    return get(DefaultConfigValue(key, defaultValue))
}

/**
 * Extension function to set typed configuration value
 */
inline fun <reified T> ConfigManager.setTyped(key: String, value: T) {
    set(DefaultConfigValue(key, value), value)
}

/**
 * Extension function to observe typed configuration value
 */
inline fun <reified T> ConfigManager.observeTyped(key: String, defaultValue: T): Flow<T> {
    return observe(DefaultConfigValue(key, defaultValue))
}