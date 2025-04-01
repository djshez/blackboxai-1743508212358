package com.example.ht2000obd.base

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.ht2000obd.R
import com.example.ht2000obd.utils.LogUtils

abstract class BaseNotificationHelper(protected val context: Context) {

    protected val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }

    // Abstract properties to be implemented by child notification helpers
    abstract val channelId: String
    abstract val channelName: String
    abstract val channelDescription: String
    abstract val notificationId: Int

    init {
        createNotificationChannel()
    }

    /**
     * Create the notification channel for Android O and above
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channel = NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = channelDescription
                }
                notificationManager.createNotificationChannel(channel)
            } catch (e: Exception) {
                LogUtils.e("Notification", "Error creating notification channel", e)
            }
        }
    }

    /**
     * Build a basic notification
     */
    protected fun buildNotification(
        title: String,
        content: String,
        @DrawableRes smallIcon: Int = R.drawable.ic_notification,
        autoCancel: Boolean = true
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(smallIcon)
            .setAutoCancel(autoCancel)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
    }

    /**
     * Build a notification with an action
     */
    protected fun buildNotificationWithAction(
        title: String,
        content: String,
        actionTitle: String,
        actionIntent: PendingIntent,
        @DrawableRes smallIcon: Int = R.drawable.ic_notification,
        @DrawableRes actionIcon: Int? = null
    ): NotificationCompat.Builder {
        return buildNotification(title, content, smallIcon)
            .addAction(
                actionIcon ?: 0,
                actionTitle,
                actionIntent
            )
    }

    /**
     * Create a pending intent for notification
     */
    protected fun createPendingIntent(
        intent: Intent,
        requestCode: Int = 0,
        flags: Int = PendingIntent.FLAG_UPDATE_CURRENT
    ): PendingIntent {
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            flags or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Show a notification
     */
    protected fun showNotification(
        builder: NotificationCompat.Builder,
        id: Int = notificationId
    ) {
        try {
            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                notificationManager.notify(id, builder.build())
            } else {
                LogUtils.w("Notification", "Notifications are disabled")
            }
        } catch (e: Exception) {
            LogUtils.e("Notification", "Error showing notification", e)
        }
    }

    /**
     * Cancel a notification
     */
    fun cancelNotification(id: Int = notificationId) {
        try {
            notificationManager.cancel(id)
        } catch (e: Exception) {
            LogUtils.e("Notification", "Error canceling notification", e)
        }
    }

    /**
     * Cancel all notifications
     */
    fun cancelAllNotifications() {
        try {
            notificationManager.cancelAll()
        } catch (e: Exception) {
            LogUtils.e("Notification", "Error canceling all notifications", e)
        }
    }

    /**
     * Update existing notification
     */
    protected fun updateNotification(
        id: Int = notificationId,
        update: (NotificationCompat.Builder) -> NotificationCompat.Builder
    ) {
        try {
            val builder = buildNotification("", "")
            showNotification(update(builder), id)
        } catch (e: Exception) {
            LogUtils.e("Notification", "Error updating notification", e)
        }
    }

    /**
     * Check if notifications are enabled
     */
    fun areNotificationsEnabled(): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /**
     * Get notification channel settings intent
     */
    fun getNotificationSettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, channelId)
            }
        } else {
            Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra("app_package", context.packageName)
                putExtra("app_uid", context.applicationInfo.uid)
            }
        }
    }

    companion object {
        private const val NOTIFICATION_GROUP = "com.example.ht2000obd.notifications"
    }
}