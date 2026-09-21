package com.jarvis.app.scheduler

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jarvis.app.notifications.JarvisNotificationHelper
import com.jarvis.app.notifications.NotificationChannels

/**
 * WorkManager worker executing scheduled background tasks and dispatching
 * notifications to the user.
 */
class ScheduledTaskWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val title = inputData.getString(KEY_TITLE) ?: "JARVIS Task"
            val message = inputData.getString(KEY_MESSAGE) ?: "Scheduled task completed."
            val channelId = inputData.getString(KEY_CHANNEL_ID) ?: NotificationChannels.GENERAL
            val notificationId = inputData.getInt(
                KEY_NOTIFICATION_ID,
                (System.currentTimeMillis() % 100000).toInt()
            )
            val isReminder = inputData.getBoolean(KEY_IS_REMINDER, false)

            val notificationHelper = JarvisNotificationHelper(applicationContext)

            if (isReminder) {
                notificationHelper.showReminderNotification(
                    title = title,
                    message = message,
                    notificationId = notificationId
                )
            } else {
                notificationHelper.showNotification(
                    title = title,
                    message = message,
                    channelId = channelId,
                    notificationId = notificationId
                )
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val KEY_TITLE = "key_task_title"
        const val KEY_MESSAGE = "key_task_message"
        const val KEY_CHANNEL_ID = "key_channel_id"
        const val KEY_NOTIFICATION_ID = "key_notification_id"
        const val KEY_IS_REMINDER = "key_is_reminder"
    }
}
