package com.jarvis.app.brain

import com.jarvis.app.agent.AgentAction
import java.util.regex.Pattern

/**
 * Offline, rule-based fallback provider when no cloud AI provider is configured.
 * Implements basic pattern matching for common voice and text commands,
 * and provides safe default behaviors for agent planning and execution.
 */
class LocalFallbackProvider : AIProvider {

    override val name: String = "Local Fallback"
    override val isConfigured: Boolean = false

    override suspend fun understandCommand(
        command: String,
        context: Map<String, String>
    ): CommandUnderstanding {
        val trimmed = command.trim()
        val lower = trimmed.lowercase()

        val (intent, entities, confidence) = when {
            lower.contains("what do you remember") || lower.contains("recall") || lower.contains("what do you know") -> {
                Triple("recall", emptyMap<String, String>(), 0.90f)
            }
            lower.contains("quiet") || lower.contains("don't talk") || lower.contains("dont talk") || lower.contains("silence") -> {
                Triple("quiet_mode", emptyMap(), 0.90f)
            }
            lower.contains("remember") -> {
                val content = trimmed.substringAfter("remember", "").trim()
                    .removePrefix("that").removePrefix("to").trim()
                val map = if (content.isNotEmpty()) mapOf("content" to content) else emptyMap()
                Triple("remember", map, 0.85f)
            }
            lower.contains("forget") -> {
                val target = trimmed.substringAfter("forget", "").trim()
                    .removePrefix("about").removePrefix("that").trim()
                val map = if (target.isNotEmpty()) mapOf("target" to target) else emptyMap()
                Triple("forget", map, 0.85f)
            }
            lower.contains("remind") -> {
                val entities = extractReminderEntities(trimmed)
                Triple("remind", entities, 0.85f)
            }
            lower.contains("wake") -> {
                val time = extractTimePattern(trimmed)
                val map = mutableMapOf<String, String>()
                if (time.isNotEmpty()) map["time"] = time
                Triple("set_alarm", map, 0.85f)
            }
            lower.contains("open") -> {
                val app = trimmed.substringAfter("open", "").trim()
                    .removePrefix("app").removePrefix("the").trim()
                val map = if (app.isNotEmpty()) mapOf("app_name" to app) else emptyMap()
                Triple("open_app", map, 0.85f)
            }
            lower.contains("send") || lower.contains("message") -> {
                val entities = extractMessageEntities(trimmed)
                Triple("send_message", entities, 0.85f)
            }
            lower.contains("call") -> {
                val contact = trimmed.substringAfter("call", "").trim()
                    .removePrefix("to").trim()
                val map = if (contact.isNotEmpty()) mapOf("contact" to contact) else emptyMap()
                Triple("make_call", map, 0.85f)
            }
            else -> {
                Triple("unknown", emptyMap(), 0.20f)
            }
        }

        val isSensitive = intent in SENSITIVE_INTENTS

        return CommandUnderstanding(
            intent = intent,
            entities = entities,
            isSensitive = isSensitive,
            confidence = confidence,
            rawText = command
        )
    }

    override suspend fun planTask(
        goal: String,
        currentScreen: String?,
        memories: List<String>
    ): TaskPlan {
        val understanding = understandCommand(goal)
        return when (understanding.intent) {
            "open_app" -> {
                val appName = understanding.entities["app_name"] ?: "application"
                TaskPlan(
                    steps = listOf(
                        "Resolve package name for '$appName'",
                        "Launch intent for '$appName'",
                        "Verify application rendered on screen"
                    ),
                    estimatedActions = 2,
                    requiresConfirmation = false,
                    summary = "Open $appName"
                )
            }
            "send_message" -> {
                val recipient = understanding.entities["recipient"] ?: "contact"
                val body = understanding.entities["message"] ?: "message content"
                TaskPlan(
                    steps = listOf(
                        "Launch messaging application",
                        "Select recipient '$recipient'",
                        "Input message: '$body'",
                        "Request user confirmation to send message"
                    ),
                    estimatedActions = 4,
                    requiresConfirmation = true,
                    summary = "Send message to $recipient"
                )
            }
            "make_call" -> {
                val contact = understanding.entities["contact"] ?: "contact"
                TaskPlan(
                    steps = listOf(
                        "Open phone dialer",
                        "Lookup contact '$contact'",
                        "Request user confirmation to place call",
                        "Place voice call to $contact"
                    ),
                    estimatedActions = 3,
                    requiresConfirmation = true,
                    summary = "Place call to $contact"
                )
            }
            "set_alarm" -> {
                val time = understanding.entities["time"] ?: "requested time"
                TaskPlan(
                    steps = listOf(
                        "Open alarm clock service",
                        "Configure alarm for $time",
                        "Save and activate alarm"
                    ),
                    estimatedActions = 2,
                    requiresConfirmation = false,
                    summary = "Set alarm for $time"
                )
            }
            "remind" -> {
                val time = understanding.entities["time"] ?: "requested time"
                val task = understanding.entities["task"] ?: goal
                TaskPlan(
                    steps = listOf(
                        "Schedule reminder notification for $time",
                        "Store reminder details in database"
                    ),
                    estimatedActions = 2,
                    requiresConfirmation = false,
                    summary = "Remind to '$task' at $time"
                )
            }
            "remember" -> {
                TaskPlan(
                    steps = listOf(
                        "Extract key information from input",
                        "Persist fact into memory repository"
                    ),
                    estimatedActions = 1,
                    requiresConfirmation = false,
                    summary = "Store memory: $goal"
                )
            }
            "forget" -> {
                TaskPlan(
                    steps = listOf(
                        "Find matching memory in repository",
                        "Request user confirmation to delete memory",
                        "Remove memory from repository"
                    ),
                    estimatedActions = 2,
                    requiresConfirmation = true,
                    summary = "Delete memory: $goal"
                )
            }
            "recall" -> {
                TaskPlan(
                    steps = listOf(
                        "Query memories from database",
                        "Present active memories"
                    ),
                    estimatedActions = 1,
                    requiresConfirmation = false,
                    summary = "Recall stored memories"
                )
            }
            "quiet_mode" -> {
                TaskPlan(
                    steps = listOf(
                        "Silence speech synthesis output",
                        "Enable quiet mode preference"
                    ),
                    estimatedActions = 1,
                    requiresConfirmation = false,
                    summary = "Activate quiet mode"
                )
            }
            else -> {
                TaskPlan(
                    steps = listOf(
                        "Analyze user intent: $goal",
                        "Fallback provider cannot generate complex multi-step plan. Configure AI provider in Settings for full agent capabilities."
                    ),
                    estimatedActions = 1,
                    requiresConfirmation = false,
                    summary = "Local fallback plan for: $goal"
                )
            }
        }
    }

    override suspend fun analyzeScreen(
        screenDescription: String,
        goal: String
    ): ScreenAnalysis {
        return ScreenAnalysis(
            visibleElements = emptyList(),
            relevantElements = emptyList(),
            suggestedAction = "AI provider not configured. Cannot analyze screen.",
            confidence = 0.0f
        )
    }

    override suspend fun generateResponse(
        prompt: String,
        context: Map<String, String>
    ): String {
        return "AI provider not configured. Please set up an AI provider in Settings."
    }

    override suspend fun summarizeMemory(memories: List<String>): String {
        return if (memories.isEmpty()) {
            "No memories stored."
        } else {
            memories.joinToString("\n")
        }
    }

    override suspend fun decideNextAction(
        goal: String,
        currentScreen: String,
        actionHistory: List<String>
    ): ActionDecision {
        return ActionDecision(
            action = AgentAction.FinishTask("AI provider not configured"),
            reasoning = "AI provider not configured. Local fallback cannot decide screen navigation actions.",
            confidence = 0.0f,
            isComplete = true
        )
    }

    private fun extractTimePattern(text: String): String {
        val timeRegex = Regex(
            """(?:at\s+(\d{1,2}(?::\d{2})?\s*(?:am|pm|AM|PM)?)|in\s+(\d+\s*(?:minutes?|hours?|mins?|secs?|seconds?)))""",
            RegexOption.IGNORE_CASE
        )
        val match = timeRegex.find(text)
        return match?.groupValues?.getOrNull(1)?.ifEmpty { match.groupValues.getOrNull(2) }?.trim() ?: ""
    }

    private fun extractReminderEntities(text: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val time = extractTimePattern(text)
        if (time.isNotEmpty()) {
            result["time"] = time
        }

        var task = text
        val prefixRegex = Regex("""^.*?\bremind(?:\s+me)?(?:\s+to)?\s+""", RegexOption.IGNORE_CASE)
        task = prefixRegex.replaceFirst(task, "").trim()
        if (time.isNotEmpty()) {
            task = task.replace(Regex("""\b(?:at|in)\s+""" + Pattern.quote(time), RegexOption.IGNORE_CASE), "").trim()
        }
        if (task.isNotEmpty()) {
            result["task"] = task
        }
        return result
    }

    private fun extractMessageEntities(text: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val toMatch = Regex("""(?:to\s+([a-zA-Z0-9_]+))""", RegexOption.IGNORE_CASE).find(text)
        toMatch?.let {
            val recipient = it.groupValues[1].trim()
            if (recipient.isNotEmpty()) {
                result["recipient"] = recipient
            }
        }
        val sayingMatch = Regex("""(?:saying|that|:)\s+(.+)$""", RegexOption.IGNORE_CASE).find(text)
        sayingMatch?.let {
            val message = it.groupValues[1].trim()
            if (message.isNotEmpty()) {
                result["message"] = message
            }
        }
        return result
    }

    companion object {
        private val SENSITIVE_INTENTS = setOf("send_message", "make_call")
    }
}
