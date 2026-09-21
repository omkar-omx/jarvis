package com.jarvis.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build

/**
 * Defines notification channel IDs and helper method to create channels on Android O+.
 */
object NotificationChannels {
    const val GENERAL = "jarvis_general"
    const val REMINDERS = "jarvis_reminders"
    const val AGENT_STATUS = "jarvis_agent"
    const val ALARMS = "jarvis_alarms"
    const val FOREGROUND = "jarvis_foreground"

    /**
     * Initializes all notification channels required by the JARVIS application.
     * Safe to call repeatedly as channels are updated or ignored if unchanged.
     */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val alarmAudioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channels = listOf(
                NotificationChannel(
                    GENERAL,
                    "General Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "General JARVIS notifications and updates"
                },
                NotificationChannel(
                    REMINDERS,
                    "Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Time-sensitive reminders and scheduled events"
                    enableVibration(true)
                },
                NotificationChannel(
                    AGENT_STATUS,
                    "Agent Status",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Real-time JARVIS agent status and execution progress"
                },
                NotificationChannel(
                    ALARMS,
                    "Alarms & Wake Up",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "High-priority alarms and wake-up notifications"
                    enableVibration(true)
                    setSound(alarmSoundUri, alarmAudioAttributes)
                    setBypassDnd(true)
                },
                NotificationChannel(
                    FOREGROUND,
                    "Foreground Service",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Ongoing JARVIS background service monitoring"
                    setShowBadge(false)
                }
            )

            notificationManager.createNotificationChannels(channels)
        }
    }
}
