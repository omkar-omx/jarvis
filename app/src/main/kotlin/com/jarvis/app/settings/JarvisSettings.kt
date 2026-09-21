package com.jarvis.app.settings

/**
 * Global configuration data model for JARVIS settings, controlling AI providers,
 * voice synthesis and recognition, proactive task execution, safety constraints,
 * quiet hours, and owner authentication flags.
 */
data class JarvisSettings(
    val isSetupComplete: Boolean = false,
    // User identity
    val userName: String = "",
    val fullName: String = "",
    val assistantName: String = "Jarvis",
    // AI provider
    val aiProviderName: String = DEFAULT_AI_PROVIDER,
    val aiApiKey: String = "",
    val aiModelName: String = "",
    val openAiApiKey: String = "",
    // Voice
    val selectedVoice: String = "en-IN-Wavenet-C",
    val voiceLanguage: String = DEFAULT_VOICE_LANGUAGE,
    val ttsSpeed: Float = DEFAULT_TTS_SPEED,
    val ttsEnabled: Boolean = true,
    // Behavior
    val proactiveModeEnabled: Boolean = false,
    val quietModeEnabled: Boolean = false,
    val quietModeStart: String = DEFAULT_QUIET_START,
    val quietModeEnd: String = DEFAULT_QUIET_END,
    val maxActionsPerTask: Int = DEFAULT_MAX_ACTIONS,
    val taskTimeoutSeconds: Int = DEFAULT_TIMEOUT_SECONDS,
    val requireAuthForSensitive: Boolean = true,
    val autoSendMessages: Boolean = false,
    val ownerAuthenticated: Boolean = false
) {
    companion object {
        const val DEFAULT_AI_PROVIDER = "none"
        const val DEFAULT_VOICE_LANGUAGE = "en-US"
        const val DEFAULT_TTS_SPEED = 1.0f
        const val DEFAULT_QUIET_START = "22:00"
        const val DEFAULT_QUIET_END = "06:00"
        const val DEFAULT_MAX_ACTIONS = 50
        const val DEFAULT_TIMEOUT_SECONDS = 120
    }
}
