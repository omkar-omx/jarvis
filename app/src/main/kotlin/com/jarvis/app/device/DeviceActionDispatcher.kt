package com.jarvis.app.device

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import com.jarvis.app.accessibility.AccessibilityBridge
import com.jarvis.app.auth.LockscreenUnlocker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DispatchResult(
    val handled: Boolean,
    val feedback: String,
    val success: Boolean
)

/**
 * Native Android hardware and OS execution dispatcher for JARVIS (OmX Infinity).
 * Handles voice commands for app opening, flashlight control, volume,
 * screenshots, navigation, and lockscreen unlocking.
 */
object DeviceActionDispatcher {

    private const val TAG = "DeviceActionDispatcher"

    private var isTorchOn = false

    suspend fun tryDispatch(rawCommand: String, context: Context): DispatchResult? {
        val cmd = rawCommand.lowercase().trim()

        // 1. Flashlight / Torch Control
        if (cmd.contains("flashlight on") || cmd.contains("torch on") ||
            cmd.contains("torch jalao") || cmd.contains("light on") || cmd.contains("turn on flash")
        ) {
            val res = toggleTorch(context, true)
            return DispatchResult(true, if (res) "Flashlight turned on, sir." else "Failed to access camera flashlight, sir.", res)
        }

        if (cmd.contains("flashlight off") || cmd.contains("torch off") ||
            cmd.contains("torch band") || cmd.contains("light off") || cmd.contains("turn off flash")
        ) {
            val res = toggleTorch(context, false)
            return DispatchResult(true, if (res) "Flashlight turned off, sir." else "Failed to switch off flashlight, sir.", res)
        }

        // 2. Volume Controls
        if (cmd.contains("volume up") || cmd.contains("volume badhao") || cmd.contains("awaz badhao")) {
            val res = adjustVolume(context, AudioManager.ADJUST_RAISE)
            return DispatchResult(true, "Volume increased, sir.", res)
        }
        if (cmd.contains("volume down") || cmd.contains("volume kam") || cmd.contains("awaz kam")) {
            val res = adjustVolume(context, AudioManager.ADJUST_LOWER)
            return DispatchResult(true, "Volume lowered, sir.", res)
        }
        if (cmd.contains("mute") || cmd.contains("silent") || cmd.contains("awaz band")) {
            val res = adjustVolume(context, AudioManager.ADJUST_MUTE)
            return DispatchResult(true, "Media audio muted, sir.", res)
        }

        // 3. Navigation & Screenshots (via Accessibility)
        if (cmd.contains("screenshot") || cmd.contains("screen capture")) {
            val service = AccessibilityBridge.getService()
            if (service != null) {
                val ok = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
                return DispatchResult(true, if (ok) "Capturing screenshot now, sir." else "Could not trigger screenshot.", ok)
            } else {
                return DispatchResult(true, "Accessibility service is offline. Please enable it in OmX Protocols to capture screenshots.", false)
            }
        }

        if (cmd == "home" || cmd.contains("go home") || cmd.contains("home screen") || cmd.contains("home jao")) {
            val ok = AccessibilityBridge.performHome()
            return DispatchResult(true, "Returning to Home screen, sir.", ok)
        }

        if (cmd == "back" || cmd.contains("go back") || cmd.contains("piche jao") || cmd.contains("back jao")) {
            val ok = AccessibilityBridge.performBack()
            return DispatchResult(true, "Dispatched Back navigation, sir.", ok)
        }

        if (cmd.contains("recent apps") || cmd.contains("recents") || cmd.contains("recent app")) {
            val ok = AccessibilityBridge.performRecents()
            return DispatchResult(true, "Opening recent applications, sir.", ok)
        }

        if (cmd.contains("scroll down") || cmd.contains("scroll niche") || cmd.contains("niche scroll")) {
            val ok = AccessibilityBridge.performScroll("DOWN")
            return DispatchResult(true, "Scrolling down, sir.", ok)
        }

        if (cmd.contains("scroll up") || cmd.contains("upar scroll") || cmd.contains("scroll upar")) {
            val ok = AccessibilityBridge.performScroll("UP")
            return DispatchResult(true, "Scrolling up, sir.", ok)
        }

        // 4. Lockscreen Unlock
        if (cmd.contains("unlock phone") || cmd.contains("phone unlock") || cmd.contains("unlock screen") || cmd.contains("unlock device")) {
            val ok = LockscreenUnlocker.attemptUnlock(context)
            return DispatchResult(
                true,
                if (ok) "Autonomous unlock sequence executed, sir." else "Could not unlock device. Ensure PIN is configured and Accessibility is online, sir.",
                ok
            )
        }

        // 4.5. Smart WhatsApp Auto-Messaging & Contact Navigation (Memory Nickname Resolution & Autonomous Send)
        if (WhatsAppNavigator.isWhatsAppMessagingCommand(rawCommand)) {
            return WhatsAppNavigator.dispatch(rawCommand, context)
        }

        // 5. Native App Launching (Requires NO Accessibility Permission — 100% Native Intent)
        val isAppCommand = cmd.startsWith("open ") || cmd.startsWith("launch ") || cmd.startsWith("start ") ||
                cmd.startsWith("play ") || cmd.contains(" kholo") || cmd.contains(" chalao") ||
                cmd.contains(" open karo") || cmd.contains(" on karo") || cmd.contains(" chala do") ||
                cmd.startsWith("kholo ") || cmd.startsWith("chalao ")
        if (isAppCommand) {
            val appTarget = extractAppName(cmd)
            if (appTarget.isNotBlank()) {
                val launched = launchApp(context, appTarget)
                if (launched) {
                    return DispatchResult(true, "Opening ${appTarget.replaceFirstChar { it.uppercase() }} for you, sir.", true)
                }
            }
        }

        return null
    }

    private fun extractAppName(cmd: String): String {
        var clean = cmd
            .replace("open karo", "")
            .replace("on karo", "")
            .replace("chala do", "")
            .replace("open ", "")
            .replace("launch ", "")
            .replace("start ", "")
            .replace("play ", "")
            .replace("kholo ", "")
            .replace("chalao ", "")
            .replace(" kholo", "")
            .replace(" chalao", "")
            .replace(" please", "")
            .replace(" app", "")
            .trim()
        if (clean.startsWith("the ")) clean = clean.removePrefix("the ").trim()
        return clean
    }

    private fun launchApp(context: Context, appName: String): Boolean {
        return try {
            // Known popular packages (Instant launch without scanning)
            val targetPkg = when (appName) {
                "youtube", "yt" -> "com.google.android.youtube"
                "whatsapp", "wa" -> "com.whatsapp"
                "chrome", "browser", "internet", "google" -> "com.android.chrome"
                "camera", "photo" -> {
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return true
                }
                "settings" -> {
                    val intent = Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return true
                }
                "maps", "google maps" -> "com.google.android.apps.maps"
                "spotify" -> "com.spotify.music"
                "instagram", "insta" -> "com.instagram.android"
                "telegram" -> "org.telegram.messenger"
                "calculator" -> "com.google.android.calculator"
                "phone", "dialer", "call" -> {
                    val intent = Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return true
                }
                "messages", "sms" -> {
                    val intent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_APP_MESSAGING)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    return true
                }
                else -> null
            }

            if (targetPkg != null) {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(targetPkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return true
                }
            }

            // Dynamic search in installed applications
            val pm = context.packageManager
            val installedApps = pm.getInstalledApplications(0)
            for (app in installedApps) {
                val label = pm.getApplicationLabel(app).toString().lowercase()
                if (label.contains(appName) || appName.contains(label)) {
                    val intent = pm.getLaunchIntentForPackage(app.packageName)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        return true
                    }
                }
            }

            false
        } catch (e: Exception) {
            Log.e(TAG, "Error launching app: $appName", e)
            false
        }
    }

    private fun toggleTorch(context: Context, turnOn: Boolean): Boolean {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return false
            cameraManager.setTorchMode(cameraId, turnOn)
            isTorchOn = turnOn
            true
        } catch (e: Exception) {
            Log.e(TAG, "Torch toggle error", e)
            false
        }
    }

    private fun adjustVolume(context: Context, direction: Int): Boolean {
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Volume adjust error", e)
            false
        }
    }
}
