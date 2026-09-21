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

        // 0. Teach Workflow Protocol ("Sikho...", "Learn workflow...")
        if (com.jarvis.app.agent.WorkflowLearningEngine.isTeachCommand(rawCommand)) {
            return com.jarvis.app.agent.WorkflowLearningEngine.teachWorkflow(rawCommand)
        }

        // 0.5. Execute Learned Workflow Protocol (Autonomous Macro Replay for any app/website)
        val learnedResult = com.jarvis.app.agent.WorkflowLearningEngine.tryExecuteLearnedWorkflow(rawCommand, context)
        if (learnedResult != null) return learnedResult

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

        // 4.6. Smart Phone Calling & Contact Memory Resolution
        val callResult = tryDispatchCall(cmd, rawCommand, context)
        if (callResult != null) return callResult

        // 4.7. Smart SMS Messaging & Contact Memory Resolution
        val smsResult = tryDispatchSms(cmd, rawCommand, context)
        if (smsResult != null) return smsResult

        // 4.8. Smart YouTube Media Play & Search
        val ytResult = tryDispatchYouTube(cmd, rawCommand, context)
        if (ytResult != null) return ytResult

        // 4.9. Smart Google Maps Navigation
        val mapsResult = tryDispatchMaps(cmd, rawCommand, context)
        if (mapsResult != null) return mapsResult

        // 4.10. Smart Google Web Search via Browser
        val browserResult = tryDispatchBrowser(cmd, rawCommand, context)
        if (browserResult != null) return browserResult

        // 4.11. Universal On-Screen Autonomous Navigation (Click & Type in any active foreground app)
        val screenResult = tryDispatchScreenAction(cmd, rawCommand, context)
        if (screenResult != null) return screenResult

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

    private suspend fun tryDispatchCall(cmd: String, raw: String, context: Context): DispatchResult? {
        val isCall = (cmd.startsWith("call ") || cmd.contains(" ko call ") || cmd.endsWith(" ko call karo") ||
                cmd.endsWith(" ko call lagao") || cmd.contains(" phone lagao") || cmd.contains(" phone karo") ||
                cmd.startsWith("phone lagao ") || cmd.startsWith("dial ")) && !cmd.contains("whatsapp")
        if (!isCall) return null

        val target = raw
            .replace("call karo", "", ignoreCase = true)
            .replace("call lagao", "", ignoreCase = true)
            .replace("call ", "", ignoreCase = true)
            .replace("phone lagao", "", ignoreCase = true)
            .replace("phone karo", "", ignoreCase = true)
            .replace("dial ", "", ignoreCase = true)
            .replace(" ko", "", ignoreCase = true)
            .replace("please ", "", ignoreCase = true)
            .trim()

        if (target.isBlank()) {
            val intent = Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return DispatchResult(true, "Opening phone dialer for you, sir.", true)
        }

        val resolved = WhatsAppNavigator.resolveTarget(target)
        val phone = resolved.phoneNumber

        return if (!phone.isNullOrBlank()) {
            val cleanPhone = phone.replace(" ", "").replace("-", "")
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            val note = if (resolved.isResolvedFromMemory) " (resolved from Neural Vault as '$target')" else ""
            DispatchResult(true, "Dialing ${resolved.displayName}$note on $cleanPhone, sir.", true)
        } else if (target.all { it.isDigit() || it == '+' || it == ' ' || it == '-' } && target.length >= 3) {
            val cleanPhone = target.replace(" ", "").replace("-", "")
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            DispatchResult(true, "Dialing $cleanPhone, sir.", true)
        } else {
            val intent = Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            DispatchResult(true, "Opening phone dialer for ${resolved.displayName}, sir.", true)
        }
    }

    private suspend fun tryDispatchSms(cmd: String, raw: String, context: Context): DispatchResult? {
        val isSms = (cmd.startsWith("sms ") || cmd.contains("sms bhejo") || cmd.contains("sms karo") ||
                cmd.startsWith("send sms ") || cmd.startsWith("text ")) && !cmd.contains("whatsapp")
        if (!isSms) return null

        val regex1 = Regex("""(?:sms bhejo|sms karo|send sms to|text)\s*(.*?)\s*(?:ko|saying|that)?\s*(.*)""", RegexOption.IGNORE_CASE)
        val match = regex1.find(raw)
        val target = match?.groupValues?.get(1)?.trim() ?: ""
        val message = match?.groupValues?.get(2)?.removePrefix("ko ")?.removePrefix("saying ")?.trim() ?: ""

        val resolved = if (target.isNotBlank()) WhatsAppNavigator.resolveTarget(target) else null
        val phone = resolved?.phoneNumber ?: if (target.all { it.isDigit() || it == '+' }) target else ""

        val sendIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")).apply {
            if (message.isNotBlank()) putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(sendIntent)
            val targetName = resolved?.displayName ?: target.ifBlank { "recipient" }
            DispatchResult(true, "Opening SMS dispatch for $targetName with message: \"${message.ifBlank { "(blank)" }}\", sir.", true)
        } catch (e: Exception) {
            Log.e(TAG, "SMS intent failed", e)
            DispatchResult(false, "Failed to launch SMS application, sir.", false)
        }
    }

    private fun tryDispatchYouTube(cmd: String, raw: String, context: Context): DispatchResult? {
        val isYt = (cmd.contains("youtube") || cmd.contains("yt")) &&
                (cmd.contains("chalao") || cmd.contains("play") || cmd.contains("search") ||
                        cmd.contains("lagao") || cmd.contains("gana") || cmd.contains("song") || cmd.contains("video"))
        if (!isYt) return null

        val query = raw
            .replace("youtube pe", "", ignoreCase = true)
            .replace("youtube par", "", ignoreCase = true)
            .replace("on youtube", "", ignoreCase = true)
            .replace("youtube", "", ignoreCase = true)
            .replace("chalao", "", ignoreCase = true)
            .replace("play ", "", ignoreCase = true)
            .replace("search karo", "", ignoreCase = true)
            .replace("search ", "", ignoreCase = true)
            .replace("lagao", "", ignoreCase = true)
            .replace("gana chalao", "", ignoreCase = true)
            .replace("video chalao", "", ignoreCase = true)
            .trim()

        if (query.isBlank()) {
            launchApp(context, "youtube")
            return DispatchResult(true, "Opening YouTube for you, sir.", true)
        }

        val cleanQuery = Uri.encode(query)
        val ytAppUri = Uri.parse("vnd.youtube://search?query=$cleanQuery")
        val intent = Intent(Intent.ACTION_VIEW, ytAppUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            DispatchResult(true, "Searching and playing '$query' on YouTube, sir.", true)
        } catch (_: Exception) {
            val webUri = Uri.parse("https://www.youtube.com/results?search_query=$cleanQuery")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            DispatchResult(true, "Searching '$query' on YouTube for you, sir.", true)
        }
    }

    private fun tryDispatchMaps(cmd: String, raw: String, context: Context): DispatchResult? {
        val isMaps = (cmd.contains("maps") || cmd.contains("map") || cmd.contains("navigate") || cmd.contains("rasta") || cmd.contains("location")) &&
                (cmd.contains("rasta dikhao") || cmd.contains("navigate") || cmd.contains("location lagao") || cmd.contains("take me to") || cmd.contains("ka rasta"))
        if (!isMaps) return null

        var destination = raw
            .replace("maps pe", "", ignoreCase = true)
            .replace("maps par", "", ignoreCase = true)
            .replace("map pe", "", ignoreCase = true)
            .replace("navigate to", "", ignoreCase = true)
            .replace("navigate karo", "", ignoreCase = true)
            .replace("ka rasta dikhao", "", ignoreCase = true)
            .replace("rasta dikhao", "", ignoreCase = true)
            .replace("ki location lagao", "", ignoreCase = true)
            .replace("location lagao", "", ignoreCase = true)
            .replace("take me to", "", ignoreCase = true)
            .trim()

        if (destination.isBlank()) destination = "nearby petrol pump"

        val encoded = Uri.encode(destination)
        val navUri = Uri.parse("google.navigation:q=$encoded")
        val mapIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(mapIntent)
            DispatchResult(true, "Initiating Google Maps turn-by-turn navigation to $destination, sir.", true)
        } catch (_: Exception) {
            val geoUri = Uri.parse("geo:0,0?q=$encoded")
            val fallbackIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
            DispatchResult(true, "Opening location $destination on Maps, sir.", true)
        }
    }

    private fun tryDispatchBrowser(cmd: String, raw: String, context: Context): DispatchResult? {
        val isSearch = cmd.contains("google pe search") || cmd.contains("google par search") ||
                cmd.contains("chrome me search") || cmd.contains("search on google")
        if (!isSearch) return null

        val query = raw
            .replace("google pe search karo", "", ignoreCase = true)
            .replace("google par search karo", "", ignoreCase = true)
            .replace("chrome me search karo", "", ignoreCase = true)
            .replace("search on google", "", ignoreCase = true)
            .replace("search karo", "", ignoreCase = true)
            .trim()

        val webUri = Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))
        val intent = Intent(Intent.ACTION_VIEW, webUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return DispatchResult(true, "Searching Google for '$query', sir.", true)
    }

    private suspend fun tryDispatchScreenAction(cmd: String, raw: String, context: Context): DispatchResult? {
        val isClick = cmd.contains(" pe click") || cmd.contains(" par click") || cmd.startsWith("click on ") || cmd.startsWith("click ") || cmd.startsWith("tap ")
        if (isClick) {
            if (!AccessibilityBridge.isConnected.value) {
                return DispatchResult(true, "Accessibility Automation is offline. Please enable it in OmX Protocols to click on-screen buttons, sir.", false)
            }

            val targetText = raw
                .replace("pe click karo", "", ignoreCase = true)
                .replace("par click karo", "", ignoreCase = true)
                .replace("pe click", "", ignoreCase = true)
                .replace("click on ", "", ignoreCase = true)
                .replace("click ", "", ignoreCase = true)
                .replace("tap on ", "", ignoreCase = true)
                .replace("tap ", "", ignoreCase = true)
                .trim()

            val screen = AccessibilityBridge.readCurrentScreen()
            if (screen != null) {
                val node = screen.findNodeByText(targetText)
                    ?: screen.findClickableElements().firstOrNull {
                        it.text?.contains(targetText, ignoreCase = true) == true ||
                                it.contentDescription?.contains(targetText, ignoreCase = true) == true
                    }

                if (node != null) {
                    val ok = AccessibilityBridge.performTap(node.id)
                    return DispatchResult(true, if (ok) "Tapped '$targetText' on screen, sir." else "Failed to tap '$targetText', sir.", ok)
                } else {
                    return DispatchResult(true, "I could not locate '$targetText' on the current screen, sir.", false)
                }
            }
        }

        val isType = cmd.endsWith(" type karo") || cmd.startsWith("type ")
        if (isType) {
            if (!AccessibilityBridge.isConnected.value) {
                return DispatchResult(true, "Accessibility Automation is offline. Please enable it in OmX Protocols to type text, sir.", false)
            }

            val textToType = raw
                .replace("type karo", "", ignoreCase = true)
                .replace("type ", "", ignoreCase = true)
                .trim()

            val ok = AccessibilityBridge.performTypeText(textToType)
            return DispatchResult(true, if (ok) "Typed \"$textToType\" into the active field, sir." else "Could not find an active text input field, sir.", ok)
        }

        return null
    }
}
