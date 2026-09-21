package com.jarvis.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Core Accessibility Service for the JARVIS system.
 *
 * Observes on-screen changes, parses UI hierarchies into structured [ScreenInfo] models,
 * and executes physical/semantic UI actions (taps, swipes, typing, scrolls) requested by
 * the JARVIS agent or background reasoning engines.
 */
class JarvisAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "JarvisAccessService"
        private const val CONTENT_CHANGE_DEBOUNCE_MS = 150L
    }

    private lateinit var screenReader: ScreenReader
    private lateinit var uiActionExecutor: UIActionExecutor
    private var lastScreenInfo: ScreenInfo? = null

    private var currentPackageName: String? = null
    private var currentActivityName: String? = null

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var contentChangeJob: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        screenReader = ScreenReader()
        uiActionExecutor = UIActionExecutor()

        try {
            val info = serviceInfo ?: AccessibilityServiceInfo()
            info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_WINDOWS_CHANGED
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            info.flags = info.flags or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            serviceInfo = info
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring service info dynamically", e)
        }

        AccessibilityBridge.registerService(this)
        Log.i(TAG, "JarvisAccessibilityService connected and registered with AccessibilityBridge")

        // Initial screen capture
        captureScreenInfo()?.let {
            AccessibilityBridge.updateScreenInfo(it)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        event.packageName?.let {
            currentPackageName = it.toString()
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                event.className?.let {
                    currentActivityName = it.toString()
                }
                val info = captureScreenInfo()
                if (info != null) {
                    AccessibilityBridge.updateScreenInfo(info)
                }
            }

            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Debounce rapid layout passes
                contentChangeJob?.cancel()
                contentChangeJob = serviceScope.launch {
                    delay(CONTENT_CHANGE_DEBOUNCE_MS)
                    val info = captureScreenInfo()
                    if (info != null) {
                        AccessibilityBridge.updateScreenInfo(info)
                    }
                }
            }

            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                val info = captureScreenInfo()
                if (info != null) {
                    AccessibilityBridge.updateScreenInfo(info)
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "JarvisAccessibilityService interrupted")
    }

    override fun onDestroy() {
        serviceScope.cancel()
        AccessibilityBridge.unregisterService()
        Log.i(TAG, "JarvisAccessibilityService unregistered and destroyed")
        super.onDestroy()
    }

    /**
     * Reads and caches the current hierarchy from [rootInActiveWindow].
     */
    @Synchronized
    fun captureScreenInfo(): ScreenInfo? {
        val root = try {
            rootInActiveWindow
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve rootInActiveWindow", e)
            null
        } ?: return lastScreenInfo

        return try {
            val info = screenReader.readScreen(
                rootNode = root,
                packageName = currentPackageName ?: root.packageName,
                className = currentActivityName
            )
            lastScreenInfo = info
            info
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing screen info", e)
            lastScreenInfo
        } finally {
            try {
                @Suppress("DEPRECATION")
                root.recycle()
            } catch (_: Exception) {}
        }
    }

    /**
     * Performs a tap on the UI element identified by [nodeId].
     */
    suspend fun performTap(nodeId: String): Boolean {
        val info = captureScreenInfo() ?: lastScreenInfo ?: return false
        return uiActionExecutor.performTap(this, nodeId, info)
    }

    /**
     * Performs a long press on the UI element identified by [nodeId].
     */
    suspend fun performLongPress(nodeId: String): Boolean {
        val info = captureScreenInfo() ?: lastScreenInfo ?: return false
        return uiActionExecutor.performLongPress(this, nodeId, info)
    }

    /**
     * Executes a linear swipe gesture between two points on the screen.
     */
    suspend fun performSwipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long
    ): Boolean {
        return uiActionExecutor.performSwipe(this, startX, startY, endX, endY, durationMs)
    }

    /**
     * Performs a directional scroll action.
     */
    suspend fun performScroll(direction: String, nodeId: String?): Boolean {
        val info = captureScreenInfo() ?: lastScreenInfo ?: ScreenInfo("", null, emptyList())
        return uiActionExecutor.performScroll(this, direction, nodeId, info)
    }

    /**
     * Enters text into an editable element or currently focused field.
     */
    suspend fun performTypeText(text: String, nodeId: String?): Boolean {
        val info = captureScreenInfo() ?: lastScreenInfo ?: ScreenInfo("", null, emptyList())
        return uiActionExecutor.performTypeText(this, text, nodeId, info)
    }

    /**
     * Performs system back navigation.
     */
    fun performBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    /**
     * Performs system home navigation.
     */
    fun performHome(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    /**
     * Opens system overview / recent apps.
     */
    fun performRecents(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_RECENTS)
    }
}
