package com.jarvis.app.agent

import android.util.Log
import com.jarvis.app.security.EmergencyStop
import com.jarvis.app.accessibility.AccessibilityBridge
import com.jarvis.app.brain.BrainManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Message in conversation history between user and JARVIS agent.
 */
data class ConversationMessage(
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Core execution engine coordinating autonomous device interaction loops,
 * reasoning through [BrainManager], planning with [TaskPlanner], and
 * dispatching actions via [ActionExecutor].
 */
class AgentEngine(
    private val brainManager: BrainManager,
    private val actionExecutor: ActionExecutor,
    private val taskPlanner: TaskPlanner
) {
    companion object {
        private const val TAG = "AgentEngine"
        private const val DEFAULT_MAX_ACTIONS = 50
        private const val CONFIRMATION_TIMEOUT_MS = 60_000L
        private const val ACTION_STABILIZATION_DELAY_MS = 500L
    }

    /**
     * Convenience secondary constructor using singleton [BrainManager].
     */
    constructor(actionExecutor: ActionExecutor) : this(
        brainManager = BrainManager,
        actionExecutor = actionExecutor,
        taskPlanner = TaskPlanner(BrainManager)
    )

    private val _state = MutableStateFlow(AgentState.IDLE)
    val state: StateFlow<AgentState> = _state.asStateFlow()

    private val _currentTask = MutableStateFlow<AgentTask?>(null)
    val currentTask: StateFlow<AgentTask?> = _currentTask.asStateFlow()

    private val _conversationHistory = MutableStateFlow<List<ConversationMessage>>(emptyList())
    val conversationHistory: StateFlow<List<ConversationMessage>> = _conversationHistory.asStateFlow()

    private var confirmationDeferred: CompletableDeferred<Boolean>? = null

    /**
     * Entry point to process a natural language user command through the agent pipeline.
     *
     * @param command User command text.
     */
    suspend fun processCommand(command: String) {
        Log.i(TAG, "Received command: '$command'")

        // 1. Add user message to conversation
        addMessage("user", command)

        // 2. Check emergency stop
        if (EmergencyStop.isActive.value) {
            val stopMessage = "Cannot execute command: Emergency Stop is active. Please reset Emergency Stop first."
            Log.w(TAG, stopMessage)
            addMessage("jarvis", stopMessage)
            _state.value = AgentState.EMERGENCY_STOPPED
            return
        }

        if (_state.value.isActive()) {
            val busyMessage = "Agent is currently busy processing task: ${_currentTask.value?.goal}"
            Log.w(TAG, busyMessage)
            addMessage("jarvis", busyMessage)
            return
        }

        try {
            // 3. Create task via TaskPlanner
            _state.value = AgentState.PLANNING
            val task = taskPlanner.createTask(command)
            _currentTask.value = task

            // 4. Execute agent loop
            executeAgentLoop(task)

            // 5. Add response to conversation
            val responseText = task.result ?: when (task.state) {
                AgentState.COMPLETED -> "Task completed successfully."
                AgentState.EMERGENCY_STOPPED -> "Task was stopped by Emergency Stop."
                AgentState.FAILED -> "Task failed after ${task.actionCount} actions."
                else -> "Task finished with status: ${task.state.displayName()}"
            }
            addMessage("jarvis", responseText)
        } catch (e: Exception) {
            Log.e(TAG, "Unhandled exception during command processing", e)
            val errorMessage = "Task encountered an unexpected error: ${e.message ?: "Unknown error"}"
            addMessage("jarvis", errorMessage)
            _currentTask.value?.let {
                it.state = AgentState.FAILED
                it.result = errorMessage
                it.updatedAt = System.currentTimeMillis()
            }
            _state.value = AgentState.FAILED
        }
    }

    /**
     * Main autonomous loop driving screen observation, brain reasoning,
     * action dispatch, and confirmation handshakes.
     */
    private suspend fun executeAgentLoop(task: AgentTask) {
        Log.i(TAG, "Starting agent loop for task '${task.id}': ${task.goal}")
        _state.value = AgentState.PLANNING
        task.state = AgentState.PLANNING
        task.updatedAt = System.currentTimeMillis()

        val maxActions = DEFAULT_MAX_ACTIONS

        while (true) {
            // 1. Check EmergencyStop
            if (EmergencyStop.isActive.value) {
                Log.w(TAG, "Emergency Stop detected during loop execution")
                task.state = AgentState.EMERGENCY_STOPPED
                task.result = "Task terminated: Emergency Stop activated"
                task.updatedAt = System.currentTimeMillis()
                _state.value = AgentState.EMERGENCY_STOPPED
                break
            }

            // 2. Check action count < max
            if (task.actionCount >= maxActions) {
                Log.w(TAG, "Task reached maximum action limit of $maxActions")
                task.state = AgentState.FAILED
                task.result = "Task terminated: Reached maximum limit of $maxActions actions"
                task.updatedAt = System.currentTimeMillis()
                _state.value = AgentState.FAILED
                break
            }

            // 3. Set state to OBSERVING
            _state.value = AgentState.OBSERVING
            task.state = AgentState.OBSERVING
            task.updatedAt = System.currentTimeMillis()

            // 4. Read current UI via AccessibilityBridge
            val screenInfo = AccessibilityBridge.readCurrentScreen()
            val screenDesc = screenInfo?.describe() ?: "No screen content available"

            // 5. Set state to EXECUTING
            _state.value = AgentState.EXECUTING
            task.state = AgentState.EXECUTING
            task.updatedAt = System.currentTimeMillis()

            // 6. Ask brain for next action
            val decision = try {
                brainManager.decideNextAction(
                    goal = task.goal,
                    currentScreen = screenDesc,
                    actionHistory = task.actionHistory
                )
            } catch (e: Exception) {
                Log.e(TAG, "Brain reasoning failure", e)
                task.state = AgentState.FAILED
                task.result = "Failed to decide next action: ${e.message ?: "Brain error"}"
                task.updatedAt = System.currentTimeMillis()
                _state.value = AgentState.FAILED
                break
            }

            val action = decision.action
            Log.d(TAG, "Brain decided next action: ${action.describe()} (reasoning: ${decision.reasoning})")

            // 7. If action is RequestConfirmation, set WAITING_CONFIRMATION, wait
            if (action is AgentAction.RequestConfirmation) {
                _state.value = AgentState.WAITING_CONFIRMATION
                task.state = AgentState.WAITING_CONFIRMATION
                task.requiresConfirmation = true
                task.updatedAt = System.currentTimeMillis()

                val deferred = CompletableDeferred<Boolean>()
                confirmationDeferred = deferred

                val confirmed = try {
                    withTimeoutOrNull(CONFIRMATION_TIMEOUT_MS) {
                        deferred.await()
                    } ?: false
                } catch (e: Exception) {
                    false
                } finally {
                    confirmationDeferred = null
                }

                if (!confirmed) {
                    Log.w(TAG, "User rejected confirmation or request timed out")
                    task.actionHistory.add("User rejected confirmation or timed out: ${action.message}")
                    task.state = AgentState.FAILED
                    task.result = "Task halted: Confirmation was rejected or timed out"
                    task.updatedAt = System.currentTimeMillis()
                    _state.value = AgentState.FAILED
                    break
                }

                task.confirmedByOwner = true
                task.actionHistory.add("Owner confirmed: ${action.message}")
                _state.value = AgentState.EXECUTING
                task.state = AgentState.EXECUTING
                task.updatedAt = System.currentTimeMillis()
                continue
            }

            // 8. If action is FinishTask, complete
            if (action is AgentAction.FinishTask) {
                task.result = action.summary
                task.state = if (action.success) AgentState.COMPLETED else AgentState.FAILED
                task.updatedAt = System.currentTimeMillis()
                task.actionHistory.add(action.describe())
                _state.value = task.state
                Log.i(TAG, "Task completed with FinishTask: ${action.summary}")
                break
            }

            // 9. Execute action via ActionExecutor
            val result = actionExecutor.execute(action)

            // 10. Verify result
            // 11. Record action in history
            val logEntry = "${action.describe()} -> ${if (result.success) "SUCCESS" else "FAILED: ${result.message}"}"
            task.actionHistory.add(logEntry)

            // 12. Increment action count
            task.actionCount++
            task.currentStep++
            task.updatedAt = System.currentTimeMillis()

            if (decision.isComplete) {
                task.state = AgentState.COMPLETED
                task.result = decision.reasoning.takeIf { it.isNotBlank() } ?: "Task completed successfully"
                task.updatedAt = System.currentTimeMillis()
                _state.value = AgentState.COMPLETED
                break
            }

            // Stabilization delay between UI actions
            delay(ACTION_STABILIZATION_DELAY_MS)
        }

        if (!task.state.isTerminal()) {
            task.state = AgentState.COMPLETED
            task.result = task.result ?: "Task execution finished"
            task.updatedAt = System.currentTimeMillis()
            _state.value = task.state
        }

        Log.i(TAG, "Agent loop finished for task '${task.id}' with state ${task.state}")
    }

    /**
     * Confirms a pending confirmation request when the agent is in [AgentState.WAITING_CONFIRMATION].
     */
    fun confirmAction() {
        confirmationDeferred?.complete(true)
    }

    /**
     * Rejects a pending confirmation request when the agent is in [AgentState.WAITING_CONFIRMATION].
     */
    fun rejectAction() {
        confirmationDeferred?.complete(false)
    }

    /**
     * Immediately halts any running task, cancels pending confirmations,
     * engages [EmergencyStop], and transitions to [AgentState.EMERGENCY_STOPPED].
     */
    fun emergencyStop() {
        Log.w(TAG, "Emergency Stop triggered")
        EmergencyStop.activate()
        confirmationDeferred?.complete(false)
        _currentTask.value?.let { task ->
            task.state = AgentState.EMERGENCY_STOPPED
            task.result = "Emergency Stop activated by user"
            task.updatedAt = System.currentTimeMillis()
        }
        _state.value = AgentState.EMERGENCY_STOPPED
    }

    /**
     * Clears the current task and resets state back to [AgentState.IDLE].
     */
    fun reset() {
        Log.i(TAG, "Resetting agent engine to IDLE")
        EmergencyStop.reset()
        confirmationDeferred?.cancel()
        _currentTask.value = null
        _state.value = AgentState.IDLE
    }

    /**
     * Clears all recorded conversation messages.
     */
    fun clearConversation() {
        _conversationHistory.value = emptyList()
    }

    private fun addMessage(role: String, content: String) {
        val message = ConversationMessage(role = role, content = content)
        _conversationHistory.value = _conversationHistory.value + message
    }
}
