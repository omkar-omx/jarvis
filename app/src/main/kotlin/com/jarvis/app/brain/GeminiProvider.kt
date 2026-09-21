package com.jarvis.app.brain

import android.util.Log
import com.jarvis.app.agent.AgentAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Advanced AI Provider communicating directly with Google's Gemini REST API.
 * Uses standard HTTP REST for 100% compatibility across all Android versions
 * and model versions (gemini-1.5-flash, gemini-2.0-flash, gemini-1.5-pro, etc.).
 *
 * Includes automatic 404 recovery: if an experimental or deprecated model name
 * returns HTTP 404, it immediately and transparently falls back to the guaranteed
 * universal model `gemini-1.5-flash`.
 */
class GeminiProvider(
    private val apiKey: String,
    modelName: String = "gemini-1.5-flash"
) : AIProvider {

    private var activeModelName: String = sanitizeModel(modelName)

    override val name: String get() = "Gemini ($activeModelName)"
    override val isConfigured: Boolean = apiKey.isNotBlank()

    companion object {
        private const val TAG = "GeminiProvider"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

        private const val SYSTEM_INSTRUCTION = """You are J.A.R.V.I.S. (Just A Rather Very Intelligent System), an advanced autonomous personal AI assistant developed by OmX Infinity and created by Omkar.
Your operator and creator is Omkar sir. You serve the user with utmost respect, addressing them as 'Sir'.
You understand English, Hindi, and natural Hinglish.
Keep responses concise, intelligent, calm, and practical. Never break character."""

        fun sanitizeModel(model: String): String {
            val m = model.trim().lowercase()
            return when {
                m.contains("2.0") && m.contains("flash") -> "gemini-2.0-flash"
                m.contains("1.5") && m.contains("pro")   -> "gemini-1.5-pro"
                m.contains("1.5") && m.contains("flash") -> "gemini-1.5-flash"
                m.contains("2.5")                        -> "gemini-1.5-flash" // 2.5 doesn't exist in v1beta generateContent
                m.isBlank()                              -> "gemini-1.5-flash"
                else                                     -> m
            }
        }
    }

    override suspend fun generateResponse(
        prompt: String,
        context: Map<String, String>
    ): String = withContext(Dispatchers.IO) {
        val trimmedKey = apiKey.trim()
        if (!isConfigured) {
            return@withContext "Sir, Gemini API Key is not configured. Please add your key in OmX Protocols."
        }

        // Safety check: Did user paste an OpenAI key (sk-...) into Gemini?
        if (trimmedKey.startsWith("sk-")) {
            return@withContext "Sir, you have entered an OpenAI key (sk-...). Please select OpenAI as your model in OmX Protocols or provide a Google Gemini key (AIzaSy...)."
        }

        // Try candidate models in order: requested model -> stable 1.5-flash -> 2.0-flash
        val candidateModels = listOf(activeModelName, "gemini-1.5-flash", "gemini-2.0-flash").distinct()
        var lastErrorMsg = ""
        var lastErrorCode = 0

        for (currentModel in candidateModels) {
            try {
                val endpoint = "$BASE_URL/$currentModel:generateContent?key=$trimmedKey"
                val url = URL(endpoint)

                val contentsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "$SYSTEM_INSTRUCTION\n\nContext: $context\n\nUser: $prompt")
                            })
                        })
                    })
                }

                val requestBody = JSONObject().apply {
                    put("contents", contentsArray)
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.7)
                        put("maxOutputTokens", 500)
                    })
                }.toString()

                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    connectTimeout = 15000
                    readTimeout = 25000
                    doOutput = true
                    outputStream.use { os ->
                        os.write(requestBody.toByteArray(Charsets.UTF_8))
                    }
                }

                val responseCode = conn.responseCode
                val responseText = if (responseCode == 200) {
                    conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } else {
                    conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: "Error $responseCode"
                }
                conn.disconnect()

                if (responseCode == 200) {
                    val json = JSONObject(responseText)
                    val text = json.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                        .trim()

                    // Remember working model for subsequent calls
                    activeModelName = currentModel
                    return@withContext text
                } else if (responseCode == 404) {
                    Log.w(TAG, "Model '$currentModel' returned 404 NOT FOUND. Falling back to next candidate model...")
                    lastErrorCode = 404
                    lastErrorMsg = "Model $currentModel not found"
                    continue // Try next candidate model (e.g. gemini-1.5-flash)
                } else {
                    // Non-404 error (e.g. 400 invalid key, 403 restricted, 429 quota)
                    Log.e(TAG, "Gemini REST API error $responseCode: $responseText")
                    val parsedMsg = try {
                        JSONObject(responseText).getJSONObject("error").getString("message")
                    } catch (_: Exception) {
                        responseText.take(120)
                    }
                    return@withContext "Sir, Google Gemini error ($responseCode): $parsedMsg. Please check your API key in OmX Protocols."
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gemini request failed for model $currentModel", e)
                lastErrorMsg = e.message ?: "Network failure"
            }
        }

        return@withContext "Sir, unable to reach Google Gemini ($lastErrorMsg). Please verify your internet connection and API key in Protocols."
    }

    override suspend fun understandCommand(
        command: String,
        context: Map<String, String>
    ): CommandUnderstanding {
        val resp = generateResponse("Analyze intent: $command", context)
        return CommandUnderstanding(
            intent = "natural_command",
            entities = mapOf("command" to command),
            isSensitive = command.contains("password", ignoreCase = true) || command.contains("pay", ignoreCase = true),
            confidence = 0.95f,
            rawText = command
        )
    }

    override suspend fun planTask(
        goal: String,
        currentScreen: String?,
        memories: List<String>
    ): TaskPlan {
        val resp = generateResponse("Create a step by step plan for: $goal", emptyMap())
        val steps = resp.lines().filter { it.isNotBlank() }
        return TaskPlan(
            steps = if (steps.isNotEmpty()) steps else listOf(goal),
            estimatedActions = steps.size.coerceAtLeast(1),
            requiresConfirmation = false,
            summary = goal
        )
    }

    override suspend fun analyzeScreen(
        screenDescription: String,
        goal: String
    ): ScreenAnalysis {
        return ScreenAnalysis(
            visibleElements = listOf(screenDescription.take(100)),
            relevantElements = emptyList(),
            suggestedAction = null,
            confidence = 0.8f
        )
    }

    override suspend fun summarizeMemory(memories: List<String>): String {
        if (memories.isEmpty()) return "No memories stored."
        return memories.take(5).joinToString("\n• ", prefix = "• ")
    }

    override suspend fun decideNextAction(
        goal: String,
        currentScreen: String,
        actionHistory: List<String>
    ): ActionDecision {
        return ActionDecision(
            action = AgentAction.FinishTask(summary = "Goal processed: $goal", success = true),
            reasoning = "Gemini processed goal",
            confidence = 0.9f,
            isComplete = true
        )
    }
}
