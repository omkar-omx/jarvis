package com.jarvis.app.accessibility

import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Traverses an [AccessibilityNodeInfo] hierarchy tree and extracts a lightweight,
 * immutable, serializable [ScreenInfo] snapshot containing [UINode] structures.
 *
 * Implements defensive limits on tree depth and total node count to prevent
 * stack overflows and excessive memory consumption on complex view hierarchies.
 */
class ScreenReader {

    companion object {
        private const val TAG = "ScreenReader"
        private const val MAX_DEPTH = 15
        private const val MAX_NODES = 500
    }

    private var nodeCounter = 0

    /**
     * Reads the current screen hierarchy starting from [rootNode].
     *
     * @param rootNode The root [AccessibilityNodeInfo] of the active window.
     * @param packageName Optional package name hint from accessibility event.
     * @param className Optional class/activity name hint from accessibility event.
     * @return A captured [ScreenInfo] snapshot of the UI hierarchy.
     */
    @Synchronized
    fun readScreen(
        rootNode: AccessibilityNodeInfo?,
        packageName: CharSequence?,
        className: CharSequence?
    ): ScreenInfo {
        nodeCounter = 0

        val pkgName = packageName?.toString()?.takeIf { it.isNotEmpty() }
            ?: rootNode?.packageName?.toString()
            ?: ""
        val actName = className?.toString()?.takeIf { it.isNotEmpty() }

        if (rootNode == null) {
            return ScreenInfo(
                packageName = pkgName,
                activityName = actName,
                nodes = emptyList(),
                timestamp = System.currentTimeMillis()
            )
        }

        val nodesList = mutableListOf<UINode>()
        try {
            val rootUiNode = nodeToUINode(
                node = rootNode,
                depth = 0,
                index = 0,
                parentId = ""
            )
            if (rootUiNode != null) {
                nodesList.add(rootUiNode)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error traversing AccessibilityNodeInfo tree", e)
        }

        return ScreenInfo(
            packageName = pkgName,
            activityName = actName,
            nodes = nodesList,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Recursively transforms an [AccessibilityNodeInfo] and its children into a [UINode].
     */
    private fun nodeToUINode(
        node: AccessibilityNodeInfo,
        depth: Int,
        index: Int,
        parentId: String
    ): UINode? {
        if (nodeCounter >= MAX_NODES) {
            return null
        }
        nodeCounter++

        val nodeId = if (parentId.isEmpty()) "node_$index" else "${parentId}_$index"

        val bounds = Rect()
        try {
            node.getBoundsInScreen(bounds)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get bounds for node: $nodeId", e)
        }

        val resId = try {
            node.viewIdResourceName
        } catch (_: Exception) {
            null
        }

        val classNameStr = try {
            node.className?.toString() ?: "android.view.View"
        } catch (_: Exception) {
            "android.view.View"
        }

        val textStr = try {
            node.text?.toString()
        } catch (_: Exception) {
            null
        }

        val contentDescStr = try {
            node.contentDescription?.toString()
        } catch (_: Exception) {
            null
        }

        val isClickable = try { node.isClickable } catch (_: Exception) { false }
        val isScrollable = try { node.isScrollable } catch (_: Exception) { false }
        val isEditable = try { node.isEditable } catch (_: Exception) { false }
        val isCheckable = try { node.isCheckable } catch (_: Exception) { false }
        val isChecked = try { node.isChecked } catch (_: Exception) { false }
        val isFocused = try { node.isFocused } catch (_: Exception) { false }

        val children = mutableListOf<UINode>()
        if (depth < MAX_DEPTH && nodeCounter < MAX_NODES) {
            val childCount = try {
                node.childCount
            } catch (_: Exception) {
                0
            }

            for (i in 0 until childCount) {
                if (nodeCounter >= MAX_NODES) break

                val childNode = try {
                    node.getChild(i)
                } catch (_: Exception) {
                    null
                } ?: continue

                try {
                    val childUiNode = nodeToUINode(
                        node = childNode,
                        depth = depth + 1,
                        index = i,
                        parentId = nodeId
                    )
                    if (childUiNode != null) {
                        children.add(childUiNode)
                    }
                } finally {
                    try {
                        @Suppress("DEPRECATION")
                        childNode.recycle()
                    } catch (_: Exception) {
                        // Safe to ignore: on API 34+ recycle is a no-op
                    }
                }
            }
        }

        return UINode(
            id = nodeId,
            className = classNameStr,
            text = textStr,
            contentDescription = contentDescStr,
            isClickable = isClickable,
            isScrollable = isScrollable,
            isEditable = isEditable,
            isCheckable = isCheckable,
            isChecked = isChecked,
            isFocused = isFocused,
            bounds = bounds,
            children = children,
            viewIdResourceName = resId
        )
    }
}
