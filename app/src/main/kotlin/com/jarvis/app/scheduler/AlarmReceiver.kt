package com.jarvis.app.scheduler

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jarvis.app.notifications.JarvisNotificationHelper

/**
 * BroadcastReceiver triggered by AlarmManager when an alarm or scheduled event fires.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra(EXTRA_ALARM_ID, DEFAULT_ALARM_ID)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "JARVIS Notification"
        val alarmType = intent.getStringExtra(EXTRA_ALARM_TYPE) ?: TYPE_ALARM

        val notificationHelper = JarvisNotificationHelper(context)

        when (alarmType) {
            TYPE_WAKE_UP -> {
                // For wake-up alarms, launch the app UI via full-screen intent
                val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

                val fullScreenPendingIntent = launchIntent?.let {
                    PendingIntent.getActivity(
                        context,
                        alarmId,
                        it,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }

                notificationHelper.showAlarmNotification(
                    title = title,
                    message = "Time to wake up. JARVIS is online and ready.",
                    notificationId = alarmId,
                    fullScreenIntent = fullScreenPendingIntent
                )
            }
            TYPE_REMINDER -> {
                notificationHelper.showReminderNotification(
                    title = "JARVIS Reminder",
                    message = title,
                    notificationId = alarmId
                )
            }
            else -> {
                notificationHelper.showAlarmNotification(
                    title = title,
                    message = "Scheduled alarm triggered",
                    notificationId = alarmId
                )
            }
        }
    }

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_TITLE = "alarm_title"
        const val EXTRA_ALARM_TYPE = "alarm_type"

        const val TYPE_ALARM = "type_alarm"
        const val TYPE_WAKE_UP = "type_wake_up"
        const val TYPE_REMINDER = "type_reminder"

        private const val DEFAULT_ALARM_ID = 100
    }
}
