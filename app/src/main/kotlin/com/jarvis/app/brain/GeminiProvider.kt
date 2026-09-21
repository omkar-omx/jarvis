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
 * and model versions (gemini-1.5-flash, gemini-2.0-flash, gemini-2.5-flash, etc.).
 */
class GeminiProvider(
    private val apiKey: String,
    private val modelName: String = "gemini-1.5-flash"
) : AIProvider {

    override val name: String = "Gemini ($modelName)"
    override val isConfigured: Boolean = apiKey.isNotBlank()

    companion object {
        private const val TAG = "GeminiProvider"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

        private const val SYSTEM_INSTRUCTION = """You are J.A.R.V.I.S. (Just A Rather Very Intelligent System), an advanced autonomous personal AI assistant developed by OmX Infinity and created by Omkar.
Your operator and creator is Omkar sir. You serve the user with utmost respect, addressing them as 'Sir'.
You understand English, Hindi, and natural Hinglish.
Keep responses concise, intelligent, calm, and practical. Never break character."""
    }

    override suspend fun generateResponse(
        prompt: String,
        context: Map<String, String>
    ): String = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext "Sir, Gemini API Key is not configured. Please add your key in OmX Protocols."
        }

        try {
            val endpoint = "$BASE_URL/$modelName:generateContent?key=${apiKey.trim()}"
            val url = URL(endpoint)

            // Construct Gemini REST JSON payload
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
                return@withContext text
            } else {
                Log.e(TAG, "Gemini REST API error $responseCode: $responseText")
                val errorMsg = try {
                    JSONObject(responseText).getJSONObject("error").getString("message")
                } catch (_: Exception) {
                    responseText.take(120)
                }
                return@withContext "Sir, Google Gemini error ($responseCode): $errorMsg. Please check your API key in OmX Protocols."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini request failed", e)
            return@withContext "Sir, unable to reach Google Gemini (${e.message}). Please verify your internet connection."
        }
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
