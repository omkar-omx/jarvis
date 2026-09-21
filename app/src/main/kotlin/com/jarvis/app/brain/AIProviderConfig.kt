package com.jarvis.app.brain

/**
 * Configuration options for AI provider integrations (OpenAI, Anthropic, Gemini, Local models, etc.).
 */
data class AIProviderConfig(
    val providerName: String = "none",
    val apiKey: String = "",
    val modelName: String = "",
    val baseUrl: String = "",
    val maxTokens: Int = 2048,
    val temperature: Float = 0.7f
) {
    val isValid: Boolean get() = providerName != "none" && apiKey.isNotBlank()
}
