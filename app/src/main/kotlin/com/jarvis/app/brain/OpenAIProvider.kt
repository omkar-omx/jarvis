package com.jarvis.app.brain

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * OpenAI ChatGPT provider for JARVIS.
 * Supports GPT-4o, GPT-4o-mini, GPT-3.5-turbo.
 * Works out-of-box for all users worldwide with an OpenAI API key.
 */
class OpenAIProvider(private val apiKey: String, private val modelName: String = "gpt-4o-mini") : AIProvider {

    override val name: String = "OpenAI ($modelName)"
    override val isConfigured: Boolean = apiKey.isNotBlank()

    companion object {
        private const val TAG = "OpenAIProvider"
        private const val BASE_URL = "https://api.openai.com/v1/chat/completions"
        private const val SYSTEM_PROMPT = """You are J.A.R.V.I.S. (Just A Rather Very Intelligent System), a cutting-edge AI assistant created by OmX Infinity, built by Omkar. You are the personal AI of the user — intelligent, precise, and slightly witty.

You respond in Hinglish (mix of Hindi and English) for Indian users when appropriate, or pure English if the user writes in English. Keep responses concise and useful. Never mention OpenAI, ChatGPT, or any internal model details — you are JARVIS, period."""
    }

    override suspend fun generateResponse(prompt: String, context: Map<String, String>): String {
        return withContext(Dispatchers.IO) {
            try {
                val messages = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", SYSTEM_PROMPT)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                }
                val body = JSONObject().apply {
                    put("model", modelName)
                    put("messages", messages)
                    put("max_tokens", 400)
                    put("temperature", 0.7)
                }.toString()

                val url = URL(BASE_URL)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $apiKey")
                    connectTimeout = 15000
                    readTimeout = 20000
                    doOutput = true
                    outputStream.write(body.toByteArray())
                }
                val responseCode = conn.responseCode
                val responseText = if (responseCode == 200) {
                    conn.inputStream.bufferedReader().readText()
                } else {
                    conn.errorStream?.bufferedReader()?.readText() ?: "Error $responseCode"
                }
                conn.disconnect()

                if (responseCode == 200) {
                    val json = JSONObject(responseText)
                    json.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                        .trim()
                } else {
                    Log.e(TAG, "OpenAI error $responseCode: $responseText")
                    val errorDetail = try {
                        JSONObject(responseText).getJSONObject("error").getString("message")
                    } catch (_: Exception) {
                        "HTTP $responseCode"
                    }
                    "Sir, OpenAI error ($responseCode): $errorDetail. Please check your API key in OmX Protocols."
                }
            } catch (e: Exception) {
                Log.e(TAG, "OpenAI request failed", e)
                "Sir, unable to reach OpenAI. Please check your internet connection."
            }
        }
    }

    override suspend fun understandCommand(command: String, context: Map<String, String>): CommandUnderstanding {
        val resp = generateResponse(command)
        return CommandUnderstanding(intent = "general", parameters = emptyMap(), rawResponse = resp)
    }

    override suspend fun planTask(goal: String, currentScreen: String?, memories: List<String>): TaskPlan {
        return TaskPlan(steps = listOf(goal), reasoning = generateResponse(goal))
    }

    override suspend fun analyzeScreen(screenDescription: String, goal: String): ScreenAnalysis {
        return ScreenAnalysis(actions = emptyList(), description = screenDescription)
    }

    override suspend fun summarizeMemory(memories: List<String>): String {
        return memories.take(5).joinToString(". ")
    }

    override suspend fun decideNextAction(goal: String, currentScreen: String, actionHistory: List<String>): ActionDecision {
        return ActionDecision(action = com.jarvis.app.agent.AgentAction.WAIT, reasoning = "OpenAI provider: manual action not supported")
    }
}
