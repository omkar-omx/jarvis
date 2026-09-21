package com.jarvis.app.notifications

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service that observes status bar notifications from other applications,
 * allowing JARVIS to monitor incoming events and context.
 */
class JarvisNotificationListener : NotificationListenerService() {

    data class NotificationInfo(
        val packageName: String,
        val title: String?,
        val text: String?,
        val timestamp: Long
    )

    private val recentNotifications = mutableListOf<NotificationInfo>()

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isEnabled.value = true
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isEnabled.value = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val sbnPackageName = sbn.packageName
        // Do not process own notifications
        if (sbnPackageName == packageName) {
            return
        }

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

        val notificationInfo = NotificationInfo(
            packageName = sbnPackageName,
            title = title,
            text = text,
            timestamp = sbn.postTime
        )

        synchronized(recentNotifications) {
            recentNotifications.add(0, notificationInfo)
            while (recentNotifications.size > MAX_RECENT_NOTIFICATIONS) {
                recentNotifications.removeAt(recentNotifications.lastIndex)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Handled when notifications are dismissed by system or user
    }

    /**
     * Returns a snapshot of the most recently received notifications.
     */
    fun getRecentNotifications(): List<NotificationInfo> {
        return synchronized(recentNotifications) {
            recentNotifications.toList()
        }
    }

    companion object {
        private const val MAX_RECENT_NOTIFICATIONS = 50

        private val _isEnabled = MutableStateFlow(false)
        val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

        /**
         * Checks whether notification listener permission has been granted in system settings.
         */
        fun isNotificationAccessGranted(context: Context): Boolean {
            val enabledListeners = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false

            val expectedComponent = ComponentName(context, JarvisNotificationListener::class.java).flattenToString()
            return enabledListeners.split(":").any {
                ComponentName.unflattenFromString(it) == ComponentName(context, JarvisNotificationListener::class.java) ||
                        it.contains(expectedComponent)
            }
        }
    }
}
