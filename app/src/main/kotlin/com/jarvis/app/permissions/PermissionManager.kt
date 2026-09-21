package com.jarvis.app.permissions

import android.Manifest
import android.app.AlarmManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages runtime permissions, accessibility privileges, and notification access
 * for the JARVIS application.
 */
class PermissionManager(context: Context) {

    private val appContext: Context = context.applicationContext

    private val _permissionStatus = MutableStateFlow(checkAllPermissions())
    val permissionStatus: StateFlow<PermissionStatus> = _permissionStatus.asStateFlow()

    /**
     * Inspects the system status for all required runtime permissions and privileged services.
     */
    fun checkAllPermissions(): PermissionStatus {
        val microphone = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val accessibility = isAccessibilityServiceEnabled()
        val notificationListener = isNotificationListenerEnabled()

        val notificationPost = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(appContext).areNotificationsEnabled()
        }

        val contacts = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        val phone = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        val exactAlarm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: false
        } else {
            true
        }

        return PermissionStatus(
            microphoneGranted = microphone,
            accessibilityEnabled = accessibility,
            notificationListenerEnabled = notificationListener,
            notificationPostEnabled = notificationPost,
            contactsGranted = contacts,
            phoneGranted = phone,
            exactAlarmGranted = exactAlarm
        )
    }

    /**
     * Checks if the JARVIS accessibility service is enabled in Android System Settings.
     */
    fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityEnabled = try {
            Settings.Secure.getInt(
                appContext.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (e: Settings.SettingNotFoundException) {
            0
        }

        if (accessibilityEnabled != 1) {
            return false
        }

        val enabledServices = Settings.Secure.getString(
            appContext.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)

        val targetPackage = appContext.packageName
        while (colonSplitter.hasNext()) {
            val componentString = colonSplitter.next()
            val componentName = ComponentName.unflattenFromString(componentString)
            if (componentName != null && componentName.packageName.equals(targetPackage, ignoreCase = true)) {
                return true
            }
            if (componentString.contains(targetPackage, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    /**
     * Checks if JARVIS has been granted notification listener access.
     */
    fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(
            appContext.contentResolver,
            "enabled_notification_listeners"
        )
        if (!flat.isNullOrEmpty()) {
            val names = flat.split(":")
            val targetPackage = appContext.packageName
            for (name in names) {
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null && cn.packageName.equals(targetPackage, ignoreCase = true)) {
                    return true
                }
                if (name.contains(targetPackage, ignoreCase = true)) {
                    return true
                }
            }
        }
        return NotificationManagerCompat.getEnabledListenerPackages(appContext).contains(appContext.packageName)
    }

    /**
     * Launches the system Accessibility settings screen.
     */
    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        appContext.startActivity(intent)
    }

    /**
     * Launches the system Notification Listener Access settings screen.
     */
    fun openNotificationListenerSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        appContext.startActivity(intent)
    }

    /**
     * Opens the application detail settings screen where permissions can be manually granted.
     */
    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", appContext.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        appContext.startActivity(intent)
    }

    /**
     * Opens exact alarm schedule settings screen on Android 12+ (API 31+).
     */
    fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", appContext.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
        }
    }

    /**
     * Re-evaluates all permission states and updates [permissionStatus].
     * @return the freshly evaluated [PermissionStatus].
     */
    fun refreshPermissions(): PermissionStatus {
        val current = checkAllPermissions()
        _permissionStatus.value = current
        return current
    }

    companion object {
        /**
         * List of standard runtime permissions required by the application.
         */
        val REQUIRED_RUNTIME_PERMISSIONS: Array<String> = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.READ_CONTACTS)
            add(Manifest.permission.CALL_PHONE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }
}
