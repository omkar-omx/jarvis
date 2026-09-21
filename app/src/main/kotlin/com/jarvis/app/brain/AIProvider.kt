package com.jarvis.app.brain

import com.jarvis.app.agent.AgentAction

/**
 * Common interface for AI intelligence providers in JARVIS.
 * Defines core capabilities including natural language understanding,
 * multi-step task planning, visual screen analysis, conversational response generation,
 * memory summarization, and action decision-making.
 */
interface AIProvider {
    val name: String
    val isConfigured: Boolean
    
    suspend fun understandCommand(command: String, context: Map<String, String> = emptyMap()): CommandUnderstanding
    suspend fun planTask(goal: String, currentScreen: String?, memories: List<String> = emptyList()): TaskPlan
    suspend fun analyzeScreen(screenDescription: String, goal: String): ScreenAnalysis
    suspend fun generateResponse(prompt: String, context: Map<String, String> = emptyMap()): String
    suspend fun summarizeMemory(memories: List<String>): String
    suspend fun decideNextAction(goal: String, currentScreen: String, actionHistory: List<String>): ActionDecision
}

data class CommandUnderstanding(
    val intent: String,
    val entities: Map<String, String>,
    val isSensitive: Boolean,
    val confidence: Float,
    val rawText: String
)

data class TaskPlan(
    val steps: List<String>,
    val estimatedActions: Int,
    val requiresConfirmation: Boolean,
    val summary: String
)

data class ScreenAnalysis(
    val visibleElements: List<String>,
    val relevantElements: List<String>,
    val suggestedAction: String?,
    val confidence: Float
)

data class ActionDecision(
    val action: AgentAction,
    val reasoning: String,
    val confidence: Float,
    val isComplete: Boolean
)
