package com.example.ht2000obd.base

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.ht2000obd.utils.LogUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

abstract class BasePreferences(context: Context) {

    private val gson = Gson()

    // Regular SharedPreferences instance
    protected val preferences: SharedPreferences = context.getSharedPreferences(
        getPreferenceName(),
        Context.MODE_PRIVATE
    )

    // Encrypted SharedPreferences instance for sensitive data
    protected val encryptedPreferences: SharedPreferences by lazy {
        createEncryptedPreferences(context)
    }

    // Abstract method to get preferences name
    protected abstract fun getPreferenceName(): String

    /**
     * Create encrypted shared preferences
     */
    private fun createEncryptedPreferences(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "${getPreferenceName()}_encrypted",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            LogUtils.e("Preferences", "Error creating encrypted preferences", e)
            // Fallback to regular preferences if encryption fails
            context.getSharedPreferences(
                "${getPreferenceName()}_encrypted",
                Context.MODE_PRIVATE
            )
        }
    }

    /**
     * String preferences
     */
    protected fun getString(key: String, defaultValue: String = ""): String {
        return preferences.getString(key, defaultValue) ?: defaultValue
    }

    protected fun putString(key: String, value: String) {
        preferences.edit { putString(key, value) }
    }

    /**
     * Int preferences
     */
    protected fun getInt(key: String, defaultValue: Int = 0): Int {
        return preferences.getInt(key, defaultValue)
    }

    protected fun putInt(key: String, value: Int) {
        preferences.edit { putInt(key, value) }
    }

    /**
     * Long preferences
     */
    protected fun getLong(key: String, defaultValue: Long = 0L): Long {
        return preferences.getLong(key, defaultValue)
    }

    protected fun putLong(key: String, value: Long) {
        preferences.edit { putLong(key, value) }
    }

    /**
     * Boolean preferences
     */
    protected fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return preferences.getBoolean(key, defaultValue)
    }

    protected fun putBoolean(key: String, value: Boolean) {
        preferences.edit { putBoolean(key, value) }
    }

    /**
     * Float preferences
     */
    protected fun getFloat(key: String, defaultValue: Float = 0f): Float {
        return preferences.getFloat(key, defaultValue)
    }

    protected fun putFloat(key: String, value: Float) {
        preferences.edit { putFloat(key, value) }
    }

    /**
     * Set preferences
     */
    protected fun getStringSet(key: String, defaultValue: Set<String> = emptySet()): Set<String> {
        return preferences.getStringSet(key, defaultValue) ?: defaultValue
    }

    protected fun putStringSet(key: String, value: Set<String>) {
        preferences.edit { putStringSet(key, value) }
    }

    /**
     * Object preferences using Gson
     */
    protected inline fun <reified T> getObject(key: String, defaultValue: T? = null): T? {
        return try {
            val json = getString(key)
            if (json.isEmpty()) defaultValue
            else gson.fromJson(json, T::class.java)
        } catch (e: Exception) {
            LogUtils.e("Preferences", "Error getting object for key: $key", e)
            defaultValue
        }
    }

    protected fun putObject(key: String, value: Any?) {
        try {
            val json = gson.toJson(value)
            putString(key, json)
        } catch (e: Exception) {
            LogUtils.e("Preferences", "Error putting object for key: $key", e)
        }
    }

    /**
     * List preferences using Gson
     */
    protected inline fun <reified T> getList(key: String, defaultValue: List<T> = emptyList()): List<T> {
        return try {
            val json = getString(key)
            if (json.isEmpty()) defaultValue
            else {
                val type = TypeToken.getParameterized(List::class.java, T::class.java).type
                gson.fromJson(json, type)
            }
        } catch (e: Exception) {
            LogUtils.e("Preferences", "Error getting list for key: $key", e)
            defaultValue
        }
    }

    protected fun <T> putList(key: String, value: List<T>) {
        try {
            val json = gson.toJson(value)
            putString(key, json)
        } catch (e: Exception) {
            LogUtils.e("Preferences", "Error putting list for key: $key", e)
        }
    }

    /**
     * Encrypted preferences
     */
    protected fun getEncryptedString(key: String, defaultValue: String = ""): String {
        return encryptedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    protected fun putEncryptedString(key: String, value: String) {
        encryptedPreferences.edit { putString(key, value) }
    }

    /**
     * Clear preferences
     */
    fun clear() {
        preferences.edit { clear() }
        encryptedPreferences.edit { clear() }
    }

    /**
     * Remove preference
     */
    fun remove(key: String) {
        preferences.edit { remove(key) }
        encryptedPreferences.edit { remove(key) }
    }

    /**
     * Check if preference exists
     */
    fun contains(key: String): Boolean {
        return preferences.contains(key) || encryptedPreferences.contains(key)
    }

    /**
     * Observe preference changes as Flow
     */
    protected fun <T> observePreference(
        key: String,
        defaultValue: T,
        getter: () -> T
    ): Flow<T> = flow {
        // Emit initial value
        emit(getter())

        // Register listener for changes
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == key) {
                kotlinx.coroutines.runBlocking {
                    emit(getter())
                }
            }
        }

        preferences.registerOnSharedPreferenceChangeListener(listener)

        try {
            // Keep the flow active
            kotlinx.coroutines.awaitCancellation()
        } finally {
            // Clean up listener when flow is cancelled
            withContext(Dispatchers.Main) {
                preferences.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }
    }.flowOn(Dispatchers.IO)
}