package com.jarvis.app.accessibility

import android.graphics.Rect

/**
 * Represents the current screen state captured by the accessibility service.
 *
 * @property packageName The package name of the active foreground application.
 * @property activityName The class or activity name of the active window, if available.
 * @property nodes The list of root-level UI hierarchy nodes on screen.
 * @property timestamp Epoch timestamp in milliseconds when this screen state was captured.
 */
data class ScreenInfo(
    val packageName: String,
    val activityName: String?,
    val nodes: List<UINode>,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Generates a human-readable textual representation of the UI tree,
     * suitable for feeding into an AI reasoning model or debugging.
     */
    fun describe(): String {
        val sb = StringBuilder()
        sb.appendLine("Package: $packageName")
        sb.appendLine("Activity: ${activityName ?: "unknown"}")
        sb.appendLine("Elements: ${nodes.size}")
        nodes.forEach { node ->
            sb.appendLine(node.describe(indent = 0))
        }
        return sb.toString().trimEnd()
    }

    /**
     * Finds all clickable elements recursively throughout the node tree.
     */
    fun findClickableElements(): List<UINode> {
        return nodes.flatMap { it.findClickable() }
    }

    /**
     * Finds all editable text input fields recursively throughout the node tree.
     */
    fun findEditableElements(): List<UINode> {
        return nodes.flatMap { it.findEditable() }
    }

    /**
     * Finds all scrollable containers recursively throughout the node tree.
     */
    fun findScrollableElements(): List<UINode> {
        return nodes.flatMap { it.findScrollable() }
    }

    /**
     * Finds a node by its unique generated ID.
     */
    fun findNodeById(targetId: String): UINode? {
        for (node in nodes) {
            val found = node.findNodeById(targetId)
            if (found != null) return found
        }
        return null
    }

    /**
     * Searches for a node whose text or content description matches the query.
     */
    fun findNodeByText(textQuery: String, ignoreCase: Boolean = true): UINode? {
        for (node in nodes) {
            val found = node.findNodeByText(textQuery, ignoreCase)
            if (found != null) return found
        }
        return null
    }
}

/**
 * Represents an individual UI component in the screen accessibility hierarchy.
 *
 * @property id Unique identifier generated based on hierarchy position (e.g., "node_0_1").
 * @property className Java/Kotlin class name of the view (e.g., "android.widget.Button").
 * @property text Visible text content of the element, if any.
 * @property contentDescription Accessibility content description of the element, if any.
 * @property isClickable Whether the element accepts tap/click actions.
 * @property isScrollable Whether the element can be scrolled.
 * @property isEditable Whether the element accepts text input.
 * @property isCheckable Whether the element represents a toggle, switch, or checkbox.
 * @property isChecked Current checked state if the element is checkable.
 * @property isFocused Whether the element currently holds input focus.
 * @property bounds Coordinates of the element on screen in absolute screen coordinates.
 * @property children Nested child UI nodes within this element.
 * @property viewIdResourceName Android resource ID string (e.g., "com.example:id/submit_button"), if available.
 */
data class UINode(
    val id: String,
    val className: String,
    val text: String?,
    val contentDescription: String?,
    val isClickable: Boolean,
    val isScrollable: Boolean,
    val isEditable: Boolean,
    val isCheckable: Boolean = false,
    val isChecked: Boolean = false,
    val isFocused: Boolean = false,
    val bounds: Rect?,
    val children: List<UINode> = emptyList(),
    val viewIdResourceName: String? = null
) {
    /**
     * Recursively prints the node and all children with appropriate indentation.
     */
    fun describe(indent: Int = 0): String {
        val prefix = " ".repeat(indent * 2)
        val attrs = mutableListOf<String>()
        if (isClickable) attrs.add("clickable")
        if (isScrollable) attrs.add("scrollable")
        if (isEditable) attrs.add("editable")
        if (isCheckable) attrs.add("checkable")
        if (isChecked) attrs.add("checked")
        if (isFocused) attrs.add("focused")
        val attrsStr = if (attrs.isNotEmpty()) " [${attrs.joinToString(",")}]" else ""
        val textStr = text?.take(50)?.let { " text='$it'" } ?: ""
        val descStr = contentDescription?.take(50)?.let { " desc='$it'" } ?: ""
        val resIdStr = viewIdResourceName?.let { " resId='$it'" } ?: ""
        val boundsStr = bounds?.let { " bounds=(${it.left},${it.top},${it.right},${it.bottom})" } ?: ""
        val result = "$prefix$id: $className$resIdStr$textStr$descStr$attrsStr$boundsStr"
        return if (children.isEmpty()) {
            result
        } else {
            result + "\n" + children.joinToString("\n") { it.describe(indent + 1) }
        }
    }

    /**
     * Recursively collects all clickable nodes under this subtree.
     */
    fun findClickable(): List<UINode> {
        val result = mutableListOf<UINode>()
        if (isClickable) result.add(this)
        children.forEach { result.addAll(it.findClickable()) }
        return result
    }

    /**
     * Recursively collects all editable nodes under this subtree.
     */
    fun findEditable(): List<UINode> {
        val result = mutableListOf<UINode>()
        if (isEditable) result.add(this)
        children.forEach { result.addAll(it.findEditable()) }
        return result
    }

    /**
     * Recursively collects all scrollable nodes under this subtree.
     */
    fun findScrollable(): List<UINode> {
        val result = mutableListOf<UINode>()
        if (isScrollable) result.add(this)
        children.forEach { result.addAll(it.findScrollable()) }
        return result
    }

    /**
     * Recursively searches for a node by its ID.
     */
    fun findNodeById(targetId: String): UINode? {
        if (id == targetId) return this
        for (child in children) {
            val found = child.findNodeById(targetId)
            if (found != null) return found
        }
        return null
    }

    /**
     * Recursively searches for a node matching the specified text or content description.
     */
    fun findNodeByText(textQuery: String, ignoreCase: Boolean = true): UINode? {
        if (text?.contains(textQuery, ignoreCase = ignoreCase) == true ||
            contentDescription?.contains(textQuery, ignoreCase = ignoreCase) == true
        ) {
            return this
        }
        for (child in children) {
            val found = child.findNodeByText(textQuery, ignoreCase)
            if (found != null) return found
        }
        return null
    }
}
