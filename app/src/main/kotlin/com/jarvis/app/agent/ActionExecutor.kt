package com.jarvis.app.agent

import android.content.Context
import android.content.Intent
import android.util.Log
import com.jarvis.app.security.EmergencyStop
import com.jarvis.app.accessibility.AccessibilityBridge
import kotlinx.coroutines.delay

/**
 * Result returned from executing an [AgentAction].
 *
 * @param success Whether the action completed successfully.
 * @param message Human-readable outcome description or failure cause.
 * @param data Optional supplementary metadata (e.g. element ID, snapshot content, intent details).
 */
data class ActionResult(
    val success: Boolean,
    val message: String,
    val data: Map<String, String> = emptyMap()
)

/**
 * Executes low-level [AgentAction] primitives on Android devices.
 * Routes UI automation actions to [AccessibilityBridge], intent actions to the Android system,
 * and enforces safety boundaries with [EmergencyStop].
 */
class ActionExecutor(
    private val context: Context
) {
    companion object {
        private const val TAG = "ActionExecutor"
    }

    /**
     * Executes the specified [AgentAction].
     *
     * Enforces that [EmergencyStop] is inactive before attempting any action.
     *
     * @param action The action to perform.
     * @return [ActionResult] indicating execution outcome.
     */
    suspend fun execute(action: AgentAction): ActionResult {
        // Enforce safety stop check before every action
        if (EmergencyStop.isActive.value) {
            Log.w(TAG, "Execution rejected: EmergencyStop is active")
            return ActionResult(
                success = false,
                message = "Action cancelled: Emergency Stop is active",
                data = mapOf("emergencyStop" to "true")
            )
        }

        Log.d(TAG, "Executing action: ${action.describe()}")

        return try {
            when (action) {
                is AgentAction.Tap -> executeTap(action)
                is AgentAction.LongPress -> executeLongPress(action)
                is AgentAction.Swipe -> executeSwipe(action)
                is AgentAction.Scroll -> executeScroll(action)
                is AgentAction.TypeText -> executeTypeText(action)
                is AgentAction.PressBack -> executePressBack()
                is AgentAction.ReadUI -> executeReadUI()
                is AgentAction.TakeSnapshot -> executeTakeSnapshot()
                is AgentAction.OpenIntent -> executeOpenIntent(action)
                is AgentAction.Wait -> executeWait(action)
                is AgentAction.FinishTask -> executeFinishTask(action)
                is AgentAction.RequestConfirmation -> executeRequestConfirmation(action)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during action execution: ${action.describe()}", e)
            ActionResult(
                success = false,
                message = "Execution error: ${e.message ?: e.toString()}",
                data = mapOf("error" to (e.message ?: "Unknown error"))
            )
        }
    }

    private suspend fun executeTap(action: AgentAction.Tap): ActionResult {
        val success = AccessibilityBridge.performTap(action.nodeId)
        return ActionResult(
            success = success,
            message = if (success) "Tapped '${action.description}' (${action.nodeId})" else "Failed to tap element ${action.nodeId}",
            data = mapOf("nodeId" to action.nodeId, "type" to "tap")
        )
    }

    private suspend fun executeLongPress(action: AgentAction.LongPress): ActionResult {
        val success = AccessibilityBridge.performLongPress(action.nodeId)
        return ActionResult(
            success = success,
            message = if (success) "Long pressed '${action.description}' (${action.nodeId})" else "Failed to long press element ${action.nodeId}",
            data = mapOf("nodeId" to action.nodeId, "type" to "long_press")
        )
    }

    private suspend fun executeSwipe(action: AgentAction.Swipe): ActionResult {
        val success = AccessibilityBridge.performSwipe(
            startX = action.startX,
            startY = action.startY,
            endX = action.endX,
            endY = action.endY,
            durationMs = action.durationMs
        )
        return ActionResult(
            success = success,
            message = if (success) "Swiped from (${action.startX}, ${action.startY}) to (${action.endX}, ${action.endY})" else "Failed to swipe",
            data = mapOf("type" to "swipe")
        )
    }

    private suspend fun executeScroll(action: AgentAction.Scroll): ActionResult {
        val success = AccessibilityBridge.performScroll(
            direction = action.direction.name,
            nodeId = action.nodeId
        )
        return ActionResult(
            success = success,
            message = if (success) "Scrolled ${action.direction.name}" + (action.nodeId?.let { " on $it" } ?: "") else "Failed to scroll ${action.direction.name}",
            data = mapOf("direction" to action.direction.name, "type" to "scroll")
        )
    }

    private suspend fun executeTypeText(action: AgentAction.TypeText): ActionResult {
        val success = AccessibilityBridge.performTypeText(
            text = action.text,
            nodeId = action.nodeId
        )
        return ActionResult(
            success = success,
            message = if (success) "Typed text into ${action.nodeId ?: "focused element"}" else "Failed to type text",
            data = mapOf("text" to action.text, "type" to "type_text")
        )
    }

    private suspend fun executePressBack(): ActionResult {
        val success = AccessibilityBridge.performBack()
        return ActionResult(
            success = success,
            message = if (success) "Pressed Back button" else "Failed to press Back button (Accessibility service not connected)",
            data = mapOf("type" to "press_back")
        )
    }

    private suspend fun executeReadUI(): ActionResult {
        val screenInfo = AccessibilityBridge.readCurrentScreen()
        return if (screenInfo != null) {
            ActionResult(
                success = true,
                message = "Read UI: ${screenInfo.packageName} (${screenInfo.nodes.size} root elements)",
                data = mapOf(
                    "type" to "read_ui",
                    "packageName" to screenInfo.packageName,
                    "activityName" to (screenInfo.activityName ?: ""),
                    "elementCount" to screenInfo.nodes.size.toString(),
                    "description" to screenInfo.describe()
                )
            )
        } else {
            ActionResult(
                success = false,
                message = "Failed to read UI: Accessibility service is inactive or window has no content",
                data = mapOf("type" to "read_ui")
            )
        }
    }

    private suspend fun executeTakeSnapshot(): ActionResult {
        val screenInfo = AccessibilityBridge.readCurrentScreen()
        return if (screenInfo != null) {
            ActionResult(
                success = true,
                message = "Captured UI snapshot for ${screenInfo.packageName}",
                data = mapOf(
                    "type" to "take_snapshot",
                    "packageName" to screenInfo.packageName,
                    "activityName" to (screenInfo.activityName ?: ""),
                    "snapshot" to screenInfo.describe()
                )
            )
        } else {
            ActionResult(
                success = false,
                message = "Failed to capture snapshot: Screen content unavailable",
                data = mapOf("type" to "take_snapshot")
            )
        }
    }

    private fun executeOpenIntent(action: AgentAction.OpenIntent): ActionResult {
        return try {
            val intent: Intent = if (!action.action.isNullOrBlank()) {
                Intent(action.action).apply {
                    if (action.packageName.isNotBlank()) {
                        setPackage(action.packageName)
                    }
                }
            } else {
                context.packageManager.getLaunchIntentForPackage(action.packageName)
                    ?: Intent().apply {
                        setPackage(action.packageName)
                    }
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            action.extras.forEach { (key, value) ->
                intent.putExtra(key, value)
            }

            context.startActivity(intent)
            ActionResult(
                success = true,
                message = "Opened application: ${action.packageName}",
                data = mapOf("packageName" to action.packageName, "type" to "open_intent")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch intent for package: ${action.packageName}", e)
            ActionResult(
                success = false,
                message = "Failed to open ${action.packageName}: ${e.message}",
                data = mapOf("packageName" to action.packageName, "error" to (e.message ?: "Unknown intent error"))
            )
        }
    }

    private suspend fun executeWait(action: AgentAction.Wait): ActionResult {
        val duration = action.durationMs.coerceAtLeast(0L)
        delay(duration)
        return ActionResult(
            success = true,
            message = "Waited ${duration}ms",
            data = mapOf("durationMs" to duration.toString(), "type" to "wait")
        )
    }

    private fun executeFinishTask(action: AgentAction.FinishTask): ActionResult {
        return ActionResult(
            success = action.success,
            message = action.summary,
            data = mapOf(
                "type" to "finish",
                "isCompleted" to "true",
                "summary" to action.summary
            )
        )
    }

    private fun executeRequestConfirmation(action: AgentAction.RequestConfirmation): ActionResult {
        return ActionResult(
            success = true,
            message = action.message,
            data = mapOf(
                "type" to "request_confirmation",
                "needs_confirmation" to "true",
                "confirmationMessage" to action.message,
                "actionDescription" to action.actionDescription
            )
        )
    }
}
