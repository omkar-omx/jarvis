package com.jarvis.app.brain

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.jarvis.app.agent.AgentAction

/**
 * Advanced AI Provider using Google's Gemini SDK.
 */
class GeminiProvider(private val apiKey: String) : AIProvider {
    override val name: String = "Gemini"
    override val isConfigured: Boolean = apiKey.isNotBlank()

    private val textModel: GenerativeModel? by lazy {
        if (apiKey.isNotBlank()) {
            GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = apiKey
            )
        } else null
    }

    override suspend fun understandCommand(
        command: String,
        context: Map<String, String>
    ): CommandUnderstanding {
        if (!isConfigured || textModel == null) {
            return CommandUnderstanding(
                intent = "general_query",
                entities = emptyMap(),
                isSensitive = false,
                confidence = 0.5f,
                rawText = command
            )
        }

        val prompt = """
            You are JARVIS, an autonomous personal AI assistant.
            Analyze this user command: "$command"
            Context: $context
            Identify the user's intent, whether it is sensitive (like financial/passwords/calls), and key parameters.
            Return a brief analysis.
        """.trimIndent()

        return try {
            val response = textModel?.generateContent(prompt)?.text ?: ""
            val isSensitive = response.contains("sensitive", ignoreCase = true) ||
                    command.contains("pay", ignoreCase = true) ||
                    command.contains("bank", ignoreCase = true) ||
                    command.contains("password", ignoreCase = true)

            CommandUnderstanding(
                intent = "natural_command",
                entities = mapOf("command" to command),
                isSensitive = isSensitive,
                confidence = 0.95f,
                rawText = command
            )
        } catch (e: Exception) {
            Log.e("GeminiProvider", "Error in understandCommand", e)
            CommandUnderstanding(
                intent = "unknown",
                entities = emptyMap(),
                isSensitive = false,
                confidence = 0.1f,
                rawText = command
            )
        }
    }

    override suspend fun planTask(
        goal: String,
        currentScreen: String?,
        memories: List<String>
    ): TaskPlan {
        if (!isConfigured || textModel == null) {
            return TaskPlan(
                steps = listOf("Execute goal: $goal"),
                estimatedActions = 1,
                requiresConfirmation = false,
                summary = goal
            )
        }

        val prompt = """
            Goal: $goal
            Current Screen State: $currentScreen
            User Memories: $memories
            Provide a step by step plan to accomplish this goal on an Android phone.
            Keep each step concise.
        """.trimIndent()

        return try {
            val response = textModel?.generateContent(prompt)?.text ?: ""
            val lines = response.lines().map { it.trim() }.filter { it.isNotBlank() && (it.startsWith("-") || it.firstOrNull()?.isDigit() == true) }
            val steps = if (lines.isNotEmpty()) lines else listOf("Analyze screen", "Execute actions for: $goal")

            TaskPlan(
                steps = steps,
                estimatedActions = steps.size,
                requiresConfirmation = goal.contains("delete", ignoreCase = true) || goal.contains("send", ignoreCase = true),
                summary = goal
            )
        } catch (e: Exception) {
            Log.e("GeminiProvider", "Error in planTask", e)
            TaskPlan(
                steps = listOf("Execute goal: $goal"),
                estimatedActions = 1,
                requiresConfirmation = false,
                summary = goal
            )
        }
    }

    override suspend fun analyzeScreen(
        screenDescription: String,
        goal: String
    ): ScreenAnalysis {
        if (!isConfigured || textModel == null) {
            return ScreenAnalysis(
                visibleElements = emptyList(),
                relevantElements = emptyList(),
                suggestedAction = null,
                confidence = 0.0f
            )
        }

        val prompt = """
            User Goal: $goal
            Screen Content: $screenDescription
            What elements are visible, which ones are relevant to the goal, and what should be the next action?
        """.trimIndent()

        return try {
            val response = textModel?.generateContent(prompt)?.text ?: ""
            ScreenAnalysis(
                visibleElements = listOf("Screen: ${screenDescription.take(100)}"),
                relevantElements = listOf("Analysis: ${response.take(150)}"),
                suggestedAction = response.take(200),
                confidence = 0.9f
            )
        } catch (e: Exception) {
            Log.e("GeminiProvider", "Error in analyzeScreen", e)
            ScreenAnalysis(
                visibleElements = emptyList(),
                relevantElements = emptyList(),
                suggestedAction = null,
                confidence = 0.0f
            )
        }
    }

    override suspend fun generateResponse(
        prompt: String,
        context: Map<String, String>
    ): String {
        if (!isConfigured || textModel == null) {
            return "JARVIS: AI engine not configured with a valid API key."
        }

        val fullPrompt = "You are JARVIS, Tony Stark's sophisticated personal assistant. User: $prompt\nContext: $context\nAnswer concisely and professionally."
        return try {
            textModel?.generateContent(fullPrompt)?.text ?: "I am at your service, sir."
        } catch (e: Exception) {
            Log.e("GeminiProvider", "Error generating response", e)
            "Sir, I experienced a minor network glitch communicating with my cloud neural core."
        }
    }

    override suspend fun summarizeMemory(memories: List<String>): String {
        if (memories.isEmpty()) return "No memories to summarize."
        if (!isConfigured || textModel == null) return memories.joinToString("\n")

        val prompt = "Summarize the following user facts and memories into key bullet points:\n" + memories.joinToString("\n")
        return try {
            textModel?.generateContent(prompt)?.text ?: memories.joinToString("\n")
        } catch (e: Exception) {
            memories.joinToString("\n")
        }
    }

    override suspend fun decideNextAction(
        goal: String,
        currentScreen: String,
        actionHistory: List<String>
    ): ActionDecision {
        if (!isConfigured || textModel == null) {
            return ActionDecision(
                action = AgentAction.FinishTask(summary = "Task completed (fallback)", success = true),
                reasoning = "Gemini provider not configured.",
                confidence = 0.5f,
                isComplete = true
            )
        }

        val prompt = """
            Goal: $goal
            Current Screen: $currentScreen
            Action History: $actionHistory
            Decide whether the task is complete. If complete, say 'TASK_FINISHED: <summary>'.
            Otherwise suggest the next action.
        """.trimIndent()

        return try {
            val response = textModel?.generateContent(prompt)?.text ?: ""
            if (response.contains("TASK_FINISHED", ignoreCase = true)) {
                val summary = response.substringAfter("TASK_FINISHED:").trim().take(100).ifEmpty { "Goal accomplished" }
                ActionDecision(
                    action = AgentAction.FinishTask(summary = summary, success = true),
                    reasoning = response,
                    confidence = 0.95f,
                    isComplete = true
                )
            } else {
                ActionDecision(
                    action = AgentAction.Wait(1000),
                    reasoning = response.take(200),
                    confidence = 0.85f,
                    isComplete = false
                )
            }
        } catch (e: Exception) {
            Log.e("GeminiProvider", "Error in decideNextAction", e)
            ActionDecision(
                action = AgentAction.FinishTask(summary = "Encountered error in decision loop: ${e.message}", success = false),
                reasoning = e.message ?: "Error",
                confidence = 0.0f,
                isComplete = true
            )
        }
    }
}
