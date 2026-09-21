package com.jarvis.app.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Helper class for scheduling and managing exact and repeating alarms
 * using Android's AlarmManager.
 */
class AlarmHelper(private val context: Context) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Schedules an exact alarm that fires even when the device is in idle/doze mode.
     *
     * @param id Unique identifier for the alarm and PendingIntent.
     * @param triggerTimeMillis Time in milliseconds (RTC_WAKEUP) when the alarm should fire.
     * @param title Title or description of the alarm to display.
     * @param alarmType Optional type flag (e.g. standard alarm, reminder, wake-up).
     */
    fun scheduleAlarm(
        id: Int,
        triggerTimeMillis: Long,
        title: String,
        alarmType: String = AlarmReceiver.TYPE_ALARM
    ) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, id)
            putExtra(AlarmReceiver.EXTRA_TITLE, title)
            putExtra(AlarmReceiver.EXTRA_ALARM_TYPE, alarmType)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Fall back to non-exact allow while idle if exact alarm permission is missing
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                pendingIntent
            )
        }
    }

    /**
     * Cancels an existing scheduled alarm by its identifier.
     *
     * @param id The ID of the alarm to cancel.
     */
    fun cancelAlarm(id: Int) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Schedules a repeating alarm that wakes up the device at the given interval.
     *
     * @param id Unique identifier for the repeating alarm.
     * @param triggerTimeMillis Time when the alarm should first trigger.
     * @param intervalMillis Interval between subsequent alarm triggers.
     * @param title Title of the recurring alarm.
     * @param alarmType Optional type flag.
     */
    fun scheduleRepeatingAlarm(
        id: Int,
        triggerTimeMillis: Long,
        intervalMillis: Long,
        title: String,
        alarmType: String = AlarmReceiver.TYPE_ALARM
    ) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, id)
            putExtra(AlarmReceiver.EXTRA_TITLE, title)
            putExtra(AlarmReceiver.EXTRA_ALARM_TYPE, alarmType)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerTimeMillis,
            intervalMillis,
            pendingIntent
        )
    }
}
