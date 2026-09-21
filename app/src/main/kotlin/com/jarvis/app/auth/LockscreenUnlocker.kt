package com.jarvis.app.auth

import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager
import android.util.Log
import com.jarvis.app.accessibility.AccessibilityBridge
import com.jarvis.app.security.SecureStorage
import kotlinx.coroutines.delay

/**
 * Automated Lockscreen and Keyguard Assistant.
 * Uses Accessibility gestures and stored owner PIN to unlock the device hands-free.
 */
object LockscreenUnlocker {

    private const val TAG = "LockscreenUnlocker"
    private const val KEY_UNLOCK_PIN = "jarvis_owner_unlock_pin"

    fun saveUnlockPin(pin: String) {
        SecureStorage.saveString(KEY_UNLOCK_PIN, pin)
    }

    fun getUnlockPin(): String? {
        return SecureStorage.getString(KEY_UNLOCK_PIN)
    }

    fun isDeviceLocked(context: Context): Boolean {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return km?.isKeyguardLocked == true
    }

    /**
     * Attempts to unlock the device by waking screen, swiping up, and entering the stored PIN.
     */
    suspend fun attemptUnlock(context: Context): Boolean {
        val storedPin = getUnlockPin()
        if (storedPin.isNullOrBlank()) {
            Log.w(TAG, "No unlock PIN configured in secure storage.")
            return false
        }

        val service = AccessibilityBridge.serviceInstance
        if (service == null) {
            Log.w(TAG, "Accessibility Service is offline. Cannot dispatch unlock gestures.")
            return false
        }

        try {
            // 1. Wake screen
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            val wl = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "JARVIS:UnlockWakeLock"
            )
            wl.acquire(3000)

            delay(400)

            // 2. Swipe up to reveal PIN pad
            val displayMetrics = context.resources.displayMetrics
            val startX = displayMetrics.widthPixels / 2f
            val startY = displayMetrics.heightPixels * 0.85f
            val endY = displayMetrics.heightPixels * 0.25f

            AccessibilityBridge.dispatchSwipe(startX, startY, startX, endY, 300)
            delay(800)

            // 3. Enter PIN digits by searching for digit nodes on keyguard
            val rootNode = service.rootInActiveWindow
            if (rootNode != null) {
                for (digit in storedPin) {
                    val matchingNodes = rootNode.findAccessibilityNodeInfosByText(digit.toString())
                    val target = matchingNodes.firstOrNull()
                    if (target != null) {
                        target.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
                    }
                    delay(150)
                }
            }

            if (wl.isHeld) wl.release()
            Log.i(TAG, "Unlock sequence executed successfully.")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute unlock sequence", e)
            return false
        }
    }
}
