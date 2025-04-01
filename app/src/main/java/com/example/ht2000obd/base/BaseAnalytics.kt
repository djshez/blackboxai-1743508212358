package com.example.ht2000obd.base

import android.os.Bundle
import com.example.ht2000obd.utils.LogUtils

/**
 * Interface for analytics events
 */
interface AnalyticsEvent {
    val name: String
    val params: Map<String, Any?>
        get() = emptyMap()

    fun toBundle(): Bundle = Bundle().apply {
        params.forEach { (key, value) ->
            when (value) {
                is String -> putString(key, value)
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)
                is Float -> putFloat(key, value)
                is Double -> putDouble(key, value)
                is Boolean -> putBoolean(key, value)
                is Bundle -> putBundle(key, value)
                is Array<*> -> putStringArray(key, value.map { it.toString() }.toTypedArray())
                is List<*> -> putStringArrayList(key, ArrayList(value.map { it.toString() }))
                null -> putString(key, null)
            }
        }
    }
}

/**
 * Interface for analytics tracking
 */
interface AnalyticsTracker {
    fun trackEvent(event: AnalyticsEvent)
    fun trackScreen(screenName: String, params: Map<String, Any?> = emptyMap())
    fun setUserProperty(name: String, value: String?)
    fun setUserId(userId: String?)
}

/**
 * Base analytics implementation
 */
abstract class BaseAnalytics : AnalyticsTracker {
    protected val trackers = mutableListOf<AnalyticsTracker>()

    fun addTracker(tracker: AnalyticsTracker) {
        trackers.add(tracker)
    }

    fun removeTracker(tracker: AnalyticsTracker) {
        trackers.remove(tracker)
    }

    override fun trackEvent(event: AnalyticsEvent) {
        try {
            trackers.forEach { tracker ->
                tracker.trackEvent(event)
            }
            LogUtils.d("Analytics", "Tracked event: ${event.name}")
        } catch (e: Exception) {
            LogUtils.e("Analytics", "Error tracking event: ${event.name}", e)
        }
    }

    override fun trackScreen(screenName: String, params: Map<String, Any?>) {
        try {
            trackers.forEach { tracker ->
                tracker.trackScreen(screenName, params)
            }
            LogUtils.d("Analytics", "Tracked screen: $screenName")
        } catch (e: Exception) {
            LogUtils.e("Analytics", "Error tracking screen: $screenName", e)
        }
    }

    override fun setUserProperty(name: String, value: String?) {
        try {
            trackers.forEach { tracker ->
                tracker.setUserProperty(name, value)
            }
            LogUtils.d("Analytics", "Set user property: $name = $value")
        } catch (e: Exception) {
            LogUtils.e("Analytics", "Error setting user property: $name", e)
        }
    }

    override fun setUserId(userId: String?) {
        try {
            trackers.forEach { tracker ->
                tracker.setUserId(userId)
            }
            LogUtils.d("Analytics", "Set user ID: $userId")
        } catch (e: Exception) {
            LogUtils.e("Analytics", "Error setting user ID", e)
        }
    }
}

/**
 * Common analytics events
 */
sealed class CommonAnalyticsEvents : AnalyticsEvent {
    data class ButtonClick(
        val buttonName: String,
        val screenName: String,
        override val params: Map<String, Any?> = mapOf(
            "button_name" to buttonName,
            "screen_name" to screenName
        )
    ) : CommonAnalyticsEvents() {
        override val name: String = "button_click"
    }

    data class ScreenView(
        val screenName: String,
        override val params: Map<String, Any?> = mapOf(
            "screen_name" to screenName
        )
    ) : CommonAnalyticsEvents() {
        override val name: String = "screen_view"
    }

    data class Error(
        val errorMessage: String,
        val errorCode: String? = null,
        val screenName: String? = null,
        override val params: Map<String, Any?> = mapOf(
            "error_message" to errorMessage,
            "error_code" to errorCode,
            "screen_name" to screenName
        )
    ) : CommonAnalyticsEvents() {
        override val name: String = "error"
    }

    data class UserAction(
        val action: String,
        val category: String,
        val label: String? = null,
        val value: Int? = null,
        override val params: Map<String, Any?> = mapOf(
            "action" to action,
            "category" to category,
            "label" to label,
            "value" to value
        )
    ) : CommonAnalyticsEvents() {
        override val name: String = "user_action"
    }
}

/**
 * Analytics constants
 */
object AnalyticsConstants {
    // Event categories
    const val CATEGORY_UI = "ui"
    const val CATEGORY_NAVIGATION = "navigation"
    const val CATEGORY_USER = "user"
    const val CATEGORY_SYSTEM = "system"
    const val CATEGORY_ERROR = "error"

    // Event actions
    const val ACTION_CLICK = "click"
    const val ACTION_VIEW = "view"
    const val ACTION_SUBMIT = "submit"
    const val ACTION_CANCEL = "cancel"
    const val ACTION_SUCCESS = "success"
    const val ACTION_FAILURE = "failure"

    // User properties
    const val USER_PROPERTY_THEME = "theme"
    const val USER_PROPERTY_LANGUAGE = "language"
    const val USER_PROPERTY_APP_VERSION = "app_version"
    const val USER_PROPERTY_DEVICE_TYPE = "device_type"
}