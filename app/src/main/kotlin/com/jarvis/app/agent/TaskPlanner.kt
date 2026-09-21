package com.jarvis.app.agent

import android.util.Log
import com.jarvis.app.brain.BrainManager

/**
 * Plans and structures multi-step automation tasks from natural language user commands.
 * Coordinates with [BrainManager] to infer intent, sensitivity, and execution steps.
 */
class TaskPlanner(
    private val brainManager: BrainManager = BrainManager
) {
    companion object {
        private const val TAG = "TaskPlanner"
    }

    /**
     * Converts a raw user command into a structured [AgentTask].
     *
     * @param command Natural language instruction or command from the user.
     * @param context Additional contextual metadata (e.g. current screen, active app, user preferences).
     * @return Fully populated [AgentTask] ready for execution by the agent engine.
     */
    suspend fun createTask(
        command: String,
        context: Map<String, String> = emptyMap()
    ): AgentTask {
        Log.d(TAG, "Creating task for command: '$command'")

        // 1. Understand the command intent and sensitivity
        val understanding = try {
            brainManager.understandCommand(command, context)
        } catch (e: Exception) {
            Log.e(TAG, "Error understanding command, falling back to default understanding", e)
            null
        }

        val goal = understanding?.intent?.takeIf { it.isNotBlank() && it != "unknown" }
            ?: command

        // 2. Generate task execution plan
        val currentScreen = context["currentScreen"]
        val taskPlan = try {
            brainManager.planTask(
                goal = goal,
                currentScreen = currentScreen,
                memories = emptyList()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error generating task plan from brain", e)
            null
        }

        val steps = if (!taskPlan?.steps.isNullOrEmpty()) {
            taskPlan!!.steps.toMutableList()
        } else {
            mutableListOf("Execute command: $command")
        }

        val isSensitive = understanding?.isSensitive ?: false
        val requiresConfirmation = (taskPlan?.requiresConfirmation == true) || isSensitive

        return AgentTask(
            goal = goal,
            originalCommand = command,
            steps = steps,
            actionHistory = mutableListOf(),
            state = AgentState.IDLE,
            currentStep = 0,
            actionCount = 0,
            result = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isSensitive = isSensitive,
            requiresConfirmation = requiresConfirmation,
            confirmedByOwner = false
        )
    }

    /**
     * Re-plans remaining steps of an active [AgentTask] based on the current screen state.
     *
     * @param task Active task being executed.
     * @param currentScreen Textual description or layout representation of the current screen.
     * @return The updated list of planned step descriptions.
     */
    suspend fun replan(task: AgentTask, currentScreen: String): List<String> {
        Log.d(TAG, "Replanning task '${task.id}' for goal '${task.goal}' with current screen state")

        return try {
            val newPlan = brainManager.planTask(
                goal = task.goal,
                currentScreen = currentScreen,
                memories = task.actionHistory
            )
            val newSteps = newPlan.steps
            if (newSteps.isNotEmpty()) {
                task.steps.clear()
                task.steps.addAll(newSteps)
                task.currentStep = 0
                task.updatedAt = System.currentTimeMillis()
                Log.i(TAG, "Task '${task.id}' replanned with ${newSteps.size} new steps")
            }
            task.steps
        } catch (e: Exception) {
            Log.e(TAG, "Error during task replanning", e)
            task.steps
        }
    }
}
