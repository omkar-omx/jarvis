package com.jarvis.app.agent

/**
 * Represents atomic actions that the JARVIS agent can perform on the Android device.
 */
sealed class AgentAction {
    data class Tap(val nodeId: String, val description: String = "") : AgentAction()
    data class LongPress(val nodeId: String, val description: String = "") : AgentAction()
    data class Swipe(val startX: Float, val startY: Float, val endX: Float, val endY: Float, val durationMs: Long = 300) : AgentAction()
    data class Scroll(val direction: ScrollDirection, val nodeId: String? = null) : AgentAction()
    data class TypeText(val text: String, val nodeId: String? = null) : AgentAction()
    object PressBack : AgentAction()
    data class OpenIntent(val packageName: String, val action: String? = null, val extras: Map<String, String> = emptyMap()) : AgentAction()
    data class Wait(val durationMs: Long = 1000) : AgentAction()
    object ReadUI : AgentAction()
    object TakeSnapshot : AgentAction()
    data class FinishTask(val summary: String, val success: Boolean = true) : AgentAction()
    data class RequestConfirmation(val message: String, val actionDescription: String = "") : AgentAction()

    fun describe(): String = when (this) {
        is Tap -> "Tap on: $description ($nodeId)"
        is LongPress -> "Long press on: $description ($nodeId)"
        is Swipe -> "Swipe from ($startX,$startY) to ($endX,$endY)"
        is Scroll -> "Scroll $direction" + (nodeId?.let { " in $it" } ?: "")
        is TypeText -> "Type: '${text.take(50)}'"
        is PressBack -> "Press Back"
        is OpenIntent -> "Open: $packageName"
        is Wait -> "Wait ${durationMs}ms"
        is ReadUI -> "Read current UI"
        is TakeSnapshot -> "Take UI snapshot"
        is FinishTask -> "Finish: $summary"
        is RequestConfirmation -> "Request confirmation: $message"
    }
}

enum class ScrollDirection { UP, DOWN, LEFT, RIGHT }
