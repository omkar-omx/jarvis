package com.jarvis.app.brain

import com.jarvis.app.agent.AgentAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton manager coordinating the active [AIProvider].
 * Provides fallback handling, configuration lifecycle, and delegates
 * AI operations to the currently active provider.
 */
object BrainManager : AIProvider {

    private var _activeProvider: AIProvider = LocalFallbackProvider()
    val activeProvider: AIProvider get() = _activeProvider

    override val name: String get() = _activeProvider.name
    override val isConfigured: Boolean get() = _activeProvider.isConfigured

    private val _isAIConfigured = MutableStateFlow(false)
    val isAIConfigured: StateFlow<Boolean> = _isAIConfigured.asStateFlow()

    private var currentConfig: AIProviderConfig = AIProviderConfig()

    /**
     * Configures the active AI provider with the specified [config].
     * If valid, updates provider and status. Returns true on success.
     */
    fun configureProvider(config: AIProviderConfig): Boolean {
        return if (config.isValid) {
            currentConfig = config
            _activeProvider = when(config.providerName.lowercase()) {
                "gemini" -> GeminiProvider(config.apiKey)
                else -> LocalFallbackProvider()
            }
            _isAIConfigured.value = true
            true
        } else {
            resetToFallback()
            false
        }
    }

    /**
     * Sets an [AIProvider] directly (e.g. for testing or external provider registration).
     */
    fun setProvider(provider: AIProvider) {
        _activeProvider = provider
        _isAIConfigured.value = provider.isConfigured
    }

    /**
     * Resets the active provider to [LocalFallbackProvider].
     */
    fun resetToFallback() {
        _activeProvider = LocalFallbackProvider()
        currentConfig = AIProviderConfig()
        _isAIConfigured.value = false
    }

    /**
     * Returns a human-readable status string for the current AI state.
     */
    fun getProviderStatus(): String {
        return if (_isAIConfigured.value) {
            "AI Connected"
        } else {
            "AI Offline - Provider not configured"
        }
    }

    // --- Delegate methods calling through to activeProvider ---

    override suspend fun understandCommand(
        command: String,
        context: Map<String, String>
    ): CommandUnderstanding {
        return _activeProvider.understandCommand(command, context)
    }

    override suspend fun planTask(
        goal: String,
        currentScreen: String?,
        memories: List<String>
    ): TaskPlan {
        return _activeProvider.planTask(goal, currentScreen, memories)
    }

    override suspend fun analyzeScreen(
        screenDescription: String,
        goal: String
    ): ScreenAnalysis {
        return _activeProvider.analyzeScreen(screenDescription, goal)
    }

    override suspend fun generateResponse(
        prompt: String,
        context: Map<String, String>
    ): String {
        return _activeProvider.generateResponse(prompt, context)
    }

    override suspend fun summarizeMemory(memories: List<String>): String {
        return _activeProvider.summarizeMemory(memories)
    }

    override suspend fun decideNextAction(
        goal: String,
        currentScreen: String,
        actionHistory: List<String>
    ): ActionDecision {
        return _activeProvider.decideNextAction(goal, currentScreen, actionHistory)
    }
}
