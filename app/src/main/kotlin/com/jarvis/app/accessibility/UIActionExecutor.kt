package com.jarvis.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Dispatches UI interactions (taps, gestures, text input, global actions)
 * via [AccessibilityService] and [GestureDescription].
 */
class UIActionExecutor {

    companion object {
        private const val TAG = "UIActionExecutor"
        private const val DEFAULT_TAP_DURATION_MS = 50L
        private const val DEFAULT_LONG_PRESS_DURATION_MS = 1000L
        private const val DEFAULT_SWIPE_DURATION_MS = 300L
    }

    /**
     * Performs a tap on the UI element identified by [nodeId].
     * Coordinates are derived from [screenInfo] bounds with fallback to [AccessibilityNodeInfo.ACTION_CLICK].
     */
    suspend fun performTap(
        service: AccessibilityService,
        nodeId: String,
        screenInfo: ScreenInfo
    ): Boolean {
        val node = screenInfo.findNodeById(nodeId)
        if (node?.bounds != null && !node.bounds.isEmpty) {
            val centerX = node.bounds.centerX().toFloat()
            val centerY = node.bounds.centerY().toFloat()
            val gestureSuccess = performTapAtCoordinates(service, centerX, centerY)
            if (gestureSuccess) {
                return true
            }
        }

        // Fallback: search AccessibilityNodeInfo in active window and invoke ACTION_CLICK
        return tryPerformNodeAction(
            service = service,
            nodeId = nodeId,
            uiNode = node,
            action = AccessibilityNodeInfo.ACTION_CLICK
        )
    }

    /**
     * Performs a long press on the UI element identified by [nodeId].
     */
    suspend fun performLongPress(
        service: AccessibilityService,
        nodeId: String,
        screenInfo: ScreenInfo
    ): Boolean {
        val node = screenInfo.findNodeById(nodeId)
        if (node?.bounds != null && !node.bounds.isEmpty) {
            val centerX = node.bounds.centerX().toFloat()
            val centerY = node.bounds.centerY().toFloat()
            val gestureSuccess = performLongPressAtCoordinates(
                service = service,
                x = centerX,
                y = centerY,
                durationMs = DEFAULT_LONG_PRESS_DURATION_MS
            )
            if (gestureSuccess) {
                return true
            }
        }

        // Fallback: invoke ACTION_LONG_CLICK
        return tryPerformNodeAction(
            service = service,
            nodeId = nodeId,
            uiNode = node,
            action = AccessibilityNodeInfo.ACTION_LONG_CLICK
        )
    }

    /**
     * Executes a linear swipe gesture between two points on the screen.
     */
    suspend fun performSwipe(
        service: AccessibilityService,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = DEFAULT_SWIPE_DURATION_MS
    ): Boolean {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val safeDuration = durationMs.coerceIn(50L, 5000L)
        val stroke = GestureDescription.StrokeDescription(path, 0L, safeDuration)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGestureAsync(service, gesture)
    }

    /**
     * Performs a directional scroll action on a target node or the active scrollable container.
     *
     * @param direction One of "UP", "DOWN", "LEFT", "RIGHT".
     * @param nodeId Optional ID of the specific scrollable element.
     * @param screenInfo Snapshot used to locate bounds.
     */
    suspend fun performScroll(
        service: AccessibilityService,
        direction: String,
        nodeId: String?,
        screenInfo: ScreenInfo
    ): Boolean {
        val targetNode = if (nodeId != null) {
            screenInfo.findNodeById(nodeId)
        } else {
            screenInfo.findScrollableElements().firstOrNull()
        }

        val normDirection = direction.trim().uppercase()

        // 1. Try AccessibilityNodeInfo ACTION_SCROLL_FORWARD / ACTION_SCROLL_BACKWARD
        val scrollAction = when (normDirection) {
            "DOWN", "FORWARD", "RIGHT" -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            "UP", "BACKWARD", "LEFT" -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            else -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        }

        val actionSuccess = tryPerformNodeAction(
            service = service,
            nodeId = targetNode?.id,
            uiNode = targetNode,
            action = scrollAction
        )
        if (actionSuccess) {
            return true
        }

        // 2. Gesture swipe fallback
        val bounds = targetNode?.bounds?.takeIf { !it.isEmpty } ?: getDisplayBounds(service)
        val (startX, startY, endX, endY) = when (normDirection) {
            "DOWN" -> {
                // Drag finger up to scroll down
                val cx = bounds.centerX().toFloat()
                listOf(cx, bounds.top + bounds.height() * 0.8f, cx, bounds.top + bounds.height() * 0.2f)
            }
            "UP" -> {
                // Drag finger down to scroll up
                val cx = bounds.centerX().toFloat()
                listOf(cx, bounds.top + bounds.height() * 0.2f, cx, bounds.top + bounds.height() * 0.8f)
            }
            "RIGHT" -> {
                // Drag finger left to scroll right
                val cy = bounds.centerY().toFloat()
                listOf(bounds.left + bounds.width() * 0.8f, cy, bounds.left + bounds.width() * 0.2f, cy)
            }
            "LEFT" -> {
                // Drag finger right to scroll left
                val cy = bounds.centerY().toFloat()
                listOf(bounds.left + bounds.width() * 0.2f, cy, bounds.left + bounds.width() * 0.8f, cy)
            }
            else -> {
                val cx = bounds.centerX().toFloat()
                listOf(cx, bounds.top + bounds.height() * 0.8f, cx, bounds.top + bounds.height() * 0.2f)
            }
        }

        return performSwipe(
            service = service,
            startX = startX,
            startY = startY,
            endX = endX,
            endY = endY,
            durationMs = DEFAULT_SWIPE_DURATION_MS
        )
    }

    /**
     * Types text into the designated editable node or currently focused input field.
     */
    suspend fun performTypeText(
        service: AccessibilityService,
        text: String,
        nodeId: String?,
        screenInfo: ScreenInfo
    ): Boolean {
        val targetNode = if (nodeId != null) {
            screenInfo.findNodeById(nodeId)
        } else {
            screenInfo.findEditableElements().firstOrNull()
        }

        val rootNode = service.rootInActiveWindow
        val targetNodeInfo = if (rootNode != null) {
            if (targetNode != null) {
                findAccessibilityNode(rootNode, targetNode.id, targetNode)
            } else {
                findFirstEditableNode(rootNode)
            }
        } else null

        if (targetNodeInfo != null) {
            try {
                // Request focus
                targetNodeInfo.performAction(AccessibilityNodeInfo.ACTION_FOCUS)

                // Attempt ACTION_SET_TEXT
                val args = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                }
                val setResult = targetNodeInfo.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                if (setResult) {
                    return true
                }

                // Fallback: Click to focus, copy to clipboard, and paste
                targetNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                val clipboard = service.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                if (clipboard != null) {
                    val clip = ClipData.newPlainText("JARVIS_TEXT", text)
                    clipboard.setPrimaryClip(clip)
                    val pasteResult = targetNodeInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE)
                    if (pasteResult) {
                        return true
                    }
                }
            } finally {
                try {
                    @Suppress("DEPRECATION")
                    targetNodeInfo.recycle()
                } catch (_: Exception) {}
                try {
                    @Suppress("DEPRECATION")
                    rootNode?.recycle()
                } catch (_: Exception) {}
            }
        }

        // Tap bounds coordinate fallback
        if (targetNode?.bounds != null && !targetNode.bounds.isEmpty) {
            performTapAtCoordinates(
                service = service,
                x = targetNode.bounds.centerX().toFloat(),
                y = targetNode.bounds.centerY().toFloat()
            )
            val clipboard = service.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard != null) {
                val clip = ClipData.newPlainText("JARVIS_TEXT", text)
                clipboard.setPrimaryClip(clip)
            }
            return true
        }

        return false
    }

    /**
     * Performs system back navigation.
     */
    fun performBack(service: AccessibilityService): Boolean {
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }

    /**
     * Performs system home navigation.
     */
    fun performHome(service: AccessibilityService): Boolean {
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
    }

    /**
     * Opens system overview / recent apps.
     */
    fun performRecents(service: AccessibilityService): Boolean {
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
    }

    /**
     * Performs a tap at exact physical screen coordinates.
     */
    suspend fun performTapAtCoordinates(
        service: AccessibilityService,
        x: Float,
        y: Float
    ): Boolean {
        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0L, DEFAULT_TAP_DURATION_MS)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGestureAsync(service, gesture)
    }

    /**
     * Performs a long press at exact physical screen coordinates.
     */
    suspend fun performLongPressAtCoordinates(
        service: AccessibilityService,
        x: Float,
        y: Float,
        durationMs: Long = DEFAULT_LONG_PRESS_DURATION_MS
    ): Boolean {
        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGestureAsync(service, gesture)
    }

    /**
     * Asynchronously dispatches a gesture and awaits completion.
     */
    private suspend fun dispatchGestureAsync(
        service: AccessibilityService,
        gesture: GestureDescription
    ): Boolean = suspendCancellableCoroutine { continuation ->
        val callback = object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                if (continuation.isActive) {
                    continuation.resume(true)
                }
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                if (continuation.isActive) {
                    continuation.resume(false)
                }
            }
        }

        val dispatched = try {
            service.dispatchGesture(gesture, callback, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error invoking dispatchGesture", e)
            false
        }

        if (!dispatched && continuation.isActive) {
            continuation.resume(false)
        }
    }

    /**
     * Helper to search and perform an action on a live [AccessibilityNodeInfo].
     */
    private fun tryPerformNodeAction(
        service: AccessibilityService,
        nodeId: String?,
        uiNode: UINode?,
        action: Int
    ): Boolean {
        val root = service.rootInActiveWindow ?: return false
        val nodeInfo = findAccessibilityNode(root, nodeId, uiNode)
        val result = if (nodeInfo != null) {
            try {
                nodeInfo.performAction(action)
            } finally {
                try {
                    @Suppress("DEPRECATION")
                    nodeInfo.recycle()
                } catch (_: Exception) {}
            }
        } else false

        try {
            @Suppress("DEPRECATION")
            root.recycle()
        } catch (_: Exception) {}

        return result
    }

    /**
     * Searches for a matching [AccessibilityNodeInfo] in the live active hierarchy.
     */
    private fun findAccessibilityNode(
        root: AccessibilityNodeInfo,
        nodeId: String?,
        uiNode: UINode?
    ): AccessibilityNodeInfo? {
        // 1. By resource ID
        if (!uiNode?.viewIdResourceName.isNullOrEmpty()) {
            val byId = root.findAccessibilityNodeInfosByViewId(uiNode!!.viewIdResourceName!!)
            if (!byId.isNullOrEmpty()) {
                val match = byId[0]
                for (i in 1 until byId.size) {
                    try {
                        @Suppress("DEPRECATION")
                        byId[i].recycle()
                    } catch (_: Exception) {}
                }
                return match
            }
        }

        // 2. By text if available
        if (!uiNode?.text.isNullOrEmpty()) {
            val byText = root.findAccessibilityNodeInfosByText(uiNode!!.text!!)
            if (!byText.isNullOrEmpty()) {
                val match = byText[0]
                for (i in 1 until byText.size) {
                    try {
                        @Suppress("DEPRECATION")
                        byText[i].recycle()
                    } catch (_: Exception) {}
                }
                return match
            }
        }

        // 3. By matching screen bounds
        if (uiNode?.bounds != null) {
            return searchNodeByBounds(root, uiNode.bounds)
        }

        return null
    }

    /**
     * Recursively traverses nodes looking for matching screen bounds.
     */
    private fun searchNodeByBounds(
        current: AccessibilityNodeInfo,
        targetBounds: Rect
    ): AccessibilityNodeInfo? {
        val bounds = Rect()
        try {
            current.getBoundsInScreen(bounds)
            if (bounds == targetBounds) {
                @Suppress("DEPRECATION")
                return AccessibilityNodeInfo.obtain(current)
            }
        } catch (_: Exception) {}

        for (i in 0 until current.childCount) {
            val child = try {
                current.getChild(i)
            } catch (_: Exception) {
                null
            } ?: continue

            val found = searchNodeByBounds(child, targetBounds)
            try {
                @Suppress("DEPRECATION")
                child.recycle()
            } catch (_: Exception) {}

            if (found != null) {
                return found
            }
        }

        return null
    }

    /**
     * Recursively searches for the first editable node.
     */
    private fun findFirstEditableNode(current: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (current.isEditable) {
            @Suppress("DEPRECATION")
            return AccessibilityNodeInfo.obtain(current)
        }

        for (i in 0 until current.childCount) {
            val child = try {
                current.getChild(i)
            } catch (_: Exception) {
                null
            } ?: continue

            val found = findFirstEditableNode(child)
            try {
                @Suppress("DEPRECATION")
                child.recycle()
            } catch (_: Exception) {}

            if (found != null) {
                return found
            }
        }

        return null
    }

    private fun getDisplayBounds(service: AccessibilityService): Rect {
        val metrics = service.resources.displayMetrics
        return Rect(0, 0, metrics.widthPixels, metrics.heightPixels)
    }
}
