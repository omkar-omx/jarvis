package com.jarvis.app.agent

import java.util.UUID

/**
 * Represents a multi-step automation task, including user goal, step plan,
 * action history, and safety status.
 */
data class AgentTask(
    val id: String = UUID.randomUUID().toString(),
    val goal: String,
    val originalCommand: String,
    val steps: MutableList<String> = mutableListOf(),
    val actionHistory: MutableList<String> = mutableListOf(),
    var state: AgentState = AgentState.IDLE,
    var currentStep: Int = 0,
    var actionCount: Int = 0,
    var result: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    var isSensitive: Boolean = false,
    var requiresConfirmation: Boolean = false,
    var confirmedByOwner: Boolean = false
)
