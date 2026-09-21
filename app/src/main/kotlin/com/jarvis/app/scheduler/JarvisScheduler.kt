package com.jarvis.app.scheduler

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Unified scheduler for the JARVIS application, managing alarms, reminders,
 * wake-up schedules, and WorkManager background tasks.
 */
class JarvisScheduler(private val context: Context) {

    private val alarmHelper = AlarmHelper(context)

    /**
     * Schedules a one-time reminder at the designated time.
     *
     * @param title The reminder title/message.
     * @param triggerTimeMillis The epoch millisecond time to trigger the reminder.
     * @param id Unique schedule ID (defaults to current timestamp).
     * @return The schedule ID used.
     */
    fun scheduleReminder(
        title: String,
        triggerTimeMillis: Long,
        id: Long = System.currentTimeMillis()
    ): Long {
        val intId = (id and 0x7FFFFFFF).toInt()
        alarmHelper.scheduleAlarm(
            id = intId,
            triggerTimeMillis = triggerTimeMillis,
            title = title,
            alarmType = AlarmReceiver.TYPE_REMINDER
        )
        return id
    }

    /**
     * Schedules a wake-up alarm that will launch a high-priority alarm notification
     * and full-screen intent.
     *
     * @param triggerTimeMillis Epoch millisecond time to trigger the wake-up alarm.
     * @return The schedule ID used.
     */
    fun scheduleWakeUp(triggerTimeMillis: Long): Long {
        val id = System.currentTimeMillis()
        val intId = (id and 0x7FFFFFFF).toInt()
        alarmHelper.scheduleAlarm(
            id = intId,
            triggerTimeMillis = triggerTimeMillis,
            title = "Wake Up Alarm",
            alarmType = AlarmReceiver.TYPE_WAKE_UP
        )
        return id
    }

    /**
     * Cancels an existing scheduled alarm or reminder.
     *
     * @param id The schedule identifier.
     */
    fun cancelSchedule(id: Long) {
        val intId = (id and 0x7FFFFFFF).toInt()
        alarmHelper.cancelAlarm(intId)
    }

    /**
     * Returns the WorkManager instance for queuing background tasks.
     */
    fun getWorkManager(): WorkManager = WorkManager.getInstance(context)

    /**
     * Schedules a background task via WorkManager with an optional delay.
     *
     * @param title Title of the task notification.
     * @param message Body text of the task.
     * @param delayMillis Delay in milliseconds before executing the worker.
     * @param isReminder Flag indicating if this task is a reminder.
     * @return UUID of the enqueued work request.
     */
    fun scheduleBackgroundTask(
        title: String,
        message: String,
        delayMillis: Long = 0L,
        isReminder: Boolean = false
    ): String {
        val inputData = Data.Builder()
            .putString(ScheduledTaskWorker.KEY_TITLE, title)
            .putString(ScheduledTaskWorker.KEY_MESSAGE, message)
            .putBoolean(ScheduledTaskWorker.KEY_IS_REMINDER, isReminder)
            .build()

        val workRequestBuilder = OneTimeWorkRequestBuilder<ScheduledTaskWorker>()
            .setInputData(inputData)

        if (delayMillis > 0) {
            workRequestBuilder.setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
        }

        val workRequest = workRequestBuilder.build()
        getWorkManager().enqueue(workRequest)
        return workRequest.id.toString()
    }
}
