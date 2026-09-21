package com.jarvis.app.scheduler

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Manages quiet mode periods during which non-emergency notifications and alarms
 * are silenced or suppressed.
 */
class QuietModeManager(
    private val defaultStartTime: String = "22:00",
    private val defaultEndTime: String = "06:00"
) {

    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * Checks if the current system time falls within the quiet hours range.
     * Supports both standard same-day ranges (e.g., "13:00" to "15:00")
     * and overnight ranges (e.g., "22:00" to "06:00").
     *
     * @param startTime Start of quiet time in "HH:mm" 24-hour format (inclusive).
     * @param endTime End of quiet time in "HH:mm" 24-hour format (exclusive).
     * @return True if current time is within quiet hours, false otherwise.
     */
    fun isQuietTime(
        startTime: String = defaultStartTime,
        endTime: String = defaultEndTime
    ): Boolean {
        return try {
            val start = LocalTime.parse(startTime.trim(), timeFormatter)
            val end = LocalTime.parse(endTime.trim(), timeFormatter)
            val now = LocalTime.now()

            if (start.isBefore(end)) {
                // Same day range (e.g., 09:00 to 17:00)
                !now.isBefore(start) && now.isBefore(end)
            } else if (start.isAfter(end)) {
                // Overnight range crossing midnight (e.g., 22:00 to 06:00)
                !now.isBefore(start) || now.isBefore(end)
            } else {
                // Start == End means entire 24-hour window
                true
            }
        } catch (e: DateTimeParseException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Determines if a notification should be presented to the user based on whether
     * the notification is an emergency and whether quiet mode is currently active.
     *
     * @param isEmergency Whether the notification has emergency priority.
     * @return True if the notification is permitted, false if silenced by quiet mode.
     */
    fun shouldAllowNotification(isEmergency: Boolean): Boolean {
        if (isEmergency) {
            return true
        }
        return !isQuietTime()
    }
}
