package com.jarvis.app.permissions

/**
 * Encapsulates the current permission and system access state required
 * for JARVIS operations across accessibility, voice, notifications, and phone automation.
 */
data class PermissionStatus(
    val microphoneGranted: Boolean = false,
    val accessibilityEnabled: Boolean = false,
    val notificationListenerEnabled: Boolean = false,
    val notificationPostEnabled: Boolean = false,
    val contactsGranted: Boolean = false,
    val phoneGranted: Boolean = false,
    val exactAlarmGranted: Boolean = false
) {
    /**
     * True if all essential and extended permissions have been granted.
     */
    val areAllGranted: Boolean
        get() = microphoneGranted &&
                accessibilityEnabled &&
                notificationListenerEnabled &&
                notificationPostEnabled &&
                contactsGranted &&
                phoneGranted &&
                exactAlarmGranted

    /**
     * True if core permissions (voice recognition and accessibility) are enabled.
     */
    val areCoreGranted: Boolean
        get() = microphoneGranted && accessibilityEnabled

    /**
     * Returns human-readable descriptions of permissions that have not yet been granted.
     */
    val missingPermissions: List<String>
        get() = buildList {
            if (!microphoneGranted) add("Microphone")
            if (!accessibilityEnabled) add("Accessibility Service")
            if (!notificationListenerEnabled) add("Notification Listener")
            if (!notificationPostEnabled) add("Post Notifications")
            if (!contactsGranted) add("Contacts")
            if (!phoneGranted) add("Phone")
            if (!exactAlarmGranted) add("Exact Alarm Scheduling")
        }
}
