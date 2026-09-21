package com.jarvis.app.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat

/**
 * Helper class for creating, displaying, and managing Android notifications
 * for the JARVIS application.
 */
class JarvisNotificationHelper(private val context: Context) {

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        NotificationChannels.createChannels(context)
    }

    private fun getAppIcon(): Int {
        return context.applicationInfo.icon.takeIf { it != 0 }
            ?: android.R.drawable.ic_dialog_info
    }

    /**
     * Shows a standard notification with the specified title and message on a given channel.
     */
    fun showNotification(
        title: String,
        message: String,
        channelId: String = NotificationChannels.GENERAL,
        notificationId: Int = 1
    ) {
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(getAppIcon())
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }

    /**
     * Shows a high-priority reminder notification.
     */
    fun showReminderNotification(
        title: String,
        message: String,
        notificationId: Int
    ) {
        val builder = NotificationCompat.Builder(context, NotificationChannels.REMINDERS)
            .setSmallIcon(getAppIcon())
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }

    /**
     * Shows or updates the ongoing agent status notification.
     */
    fun showAgentStatusNotification(status: String) {
        val builder = NotificationCompat.Builder(context, NotificationChannels.AGENT_STATUS)
            .setSmallIcon(getAppIcon())
            .setContentTitle("JARVIS Agent")
            .setContentText(status)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setOngoing(true)
            .setAutoCancel(false)

        notificationManager.notify(AGENT_STATUS_NOTIFICATION_ID, builder.build())
    }

    /**
     * Shows a high-priority alarm notification, optionally with a full-screen intent.
     */
    fun showAlarmNotification(
        title: String,
        message: String,
        notificationId: Int,
        fullScreenIntent: PendingIntent? = null
    ) {
        val builder = NotificationCompat.Builder(context, NotificationChannels.ALARMS)
            .setSmallIcon(getAppIcon())
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)

        if (fullScreenIntent != null) {
            builder.setFullScreenIntent(fullScreenIntent, true)
        }

        notificationManager.notify(notificationId, builder.build())
    }

    /**
     * Cancels an active notification by ID.
     */
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    /**
     * Cancels all notifications posted by this application.
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }

    companion object {
        const val AGENT_STATUS_NOTIFICATION_ID = 1001
        const val WAKE_UP_NOTIFICATION_ID = 2001
    }
}
