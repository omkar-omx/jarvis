package com.jarvis.app.settings

import android.content.Context
import android.content.SharedPreferences
import com.jarvis.app.security.SecureStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for reading and persisting JARVIS system preferences.
 * AI provider secrets are mirrored to [SecureStorage] for encrypted hardware-backed storage.
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<JarvisSettings> = _settingsFlow.asStateFlow()

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        _settingsFlow.value = loadSettings()
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    /**
     * Loads the current settings from SharedPreferences, resolving the API key
     * preferentially from [SecureStorage].
     */
    fun loadSettings(): JarvisSettings {
        val apiKey = try {
            SecureStorage.getApiKey()?.takeIf { it.isNotBlank() }
                ?: prefs.getString(KEY_AI_API_KEY, "") ?: ""
        } catch (_: Throwable) {
            prefs.getString(KEY_AI_API_KEY, "") ?: ""
        }

        return JarvisSettings(
            isSetupComplete = prefs.getBoolean(KEY_IS_SETUP_COMPLETE, false),
            userName = prefs.getString(KEY_USER_NAME, "") ?: "",
            fullName = prefs.getString(KEY_FULL_NAME, "") ?: "",
            assistantName = prefs.getString(KEY_ASSISTANT_NAME, "Jarvis") ?: "Jarvis",
            aiProviderName = prefs.getString(KEY_AI_PROVIDER_NAME, JarvisSettings.DEFAULT_AI_PROVIDER)
                ?: JarvisSettings.DEFAULT_AI_PROVIDER,
            aiApiKey = apiKey,
            aiModelName = run {
                val raw = prefs.getString(KEY_AI_MODEL_NAME, "gemini-1.5-flash") ?: "gemini-1.5-flash"
                if (raw.contains("2.5") || raw.isBlank()) "gemini-1.5-flash" else raw
            },
            openAiApiKey = prefs.getString(KEY_OPENAI_API_KEY, "") ?: "",
            selectedVoice = prefs.getString(KEY_SELECTED_VOICE, "en-IN-Wavenet-C") ?: "en-IN-Wavenet-C",
            voiceLanguage = prefs.getString(KEY_VOICE_LANGUAGE, JarvisSettings.DEFAULT_VOICE_LANGUAGE)
                ?: JarvisSettings.DEFAULT_VOICE_LANGUAGE,
            ttsSpeed = prefs.getFloat(KEY_TTS_SPEED, JarvisSettings.DEFAULT_TTS_SPEED),
            ttsEnabled = prefs.getBoolean(KEY_TTS_ENABLED, true),
            proactiveModeEnabled = prefs.getBoolean(KEY_PROACTIVE_MODE_ENABLED, false),
            quietModeEnabled = prefs.getBoolean(KEY_QUIET_MODE_ENABLED, false),
            quietModeStart = prefs.getString(KEY_QUIET_MODE_START, JarvisSettings.DEFAULT_QUIET_START)
                ?: JarvisSettings.DEFAULT_QUIET_START,
            quietModeEnd = prefs.getString(KEY_QUIET_MODE_END, JarvisSettings.DEFAULT_QUIET_END)
                ?: JarvisSettings.DEFAULT_QUIET_END,
            maxActionsPerTask = prefs.getInt(KEY_MAX_ACTIONS_PER_TASK, JarvisSettings.DEFAULT_MAX_ACTIONS),
            taskTimeoutSeconds = prefs.getInt(KEY_TASK_TIMEOUT_SECONDS, JarvisSettings.DEFAULT_TIMEOUT_SECONDS),
            requireAuthForSensitive = prefs.getBoolean(KEY_REQUIRE_AUTH_FOR_SENSITIVE, true),
            autoSendMessages = prefs.getBoolean(KEY_AUTO_SEND_MESSAGES, false),
            ownerAuthenticated = prefs.getBoolean(KEY_OWNER_AUTHENTICATED, false)
        )
    }

    /**
     * Persists all fields of [JarvisSettings], backing up the API key into [SecureStorage].
     */
    fun saveSettings(settings: JarvisSettings) {
        try {
            if (settings.aiApiKey.isNotBlank()) {
                val keyType = if (settings.aiProviderName.contains("openai", ignoreCase = true)) "openai" else "gemini"
                SecureStorage.saveApiKey(settings.aiApiKey, keyType)
            }
        } catch (_: Throwable) {
            // SecureStorage may not be initialized in test environments
        }

        prefs.edit()
            .putBoolean(KEY_IS_SETUP_COMPLETE, settings.isSetupComplete)
            .putString(KEY_USER_NAME, settings.userName)
            .putString(KEY_FULL_NAME, settings.fullName)
            .putString(KEY_ASSISTANT_NAME, settings.assistantName)
            .putString(KEY_AI_PROVIDER_NAME, settings.aiProviderName)
            .putString(KEY_AI_API_KEY, settings.aiApiKey)
            .putString(KEY_AI_MODEL_NAME, settings.aiModelName)
            .putString(KEY_OPENAI_API_KEY, settings.openAiApiKey)
            .putString(KEY_SELECTED_VOICE, settings.selectedVoice)
            .putString(KEY_VOICE_LANGUAGE, settings.voiceLanguage)
            .putFloat(KEY_TTS_SPEED, settings.ttsSpeed)
            .putBoolean(KEY_TTS_ENABLED, settings.ttsEnabled)
            .putBoolean(KEY_PROACTIVE_MODE_ENABLED, settings.proactiveModeEnabled)
            .putBoolean(KEY_QUIET_MODE_ENABLED, settings.quietModeEnabled)
            .putString(KEY_QUIET_MODE_START, settings.quietModeStart)
            .putString(KEY_QUIET_MODE_END, settings.quietModeEnd)
            .putInt(KEY_MAX_ACTIONS_PER_TASK, settings.maxActionsPerTask)
            .putInt(KEY_TASK_TIMEOUT_SECONDS, settings.taskTimeoutSeconds)
            .putBoolean(KEY_REQUIRE_AUTH_FOR_SENSITIVE, settings.requireAuthForSensitive)
            .putBoolean(KEY_AUTO_SEND_MESSAGES, settings.autoSendMessages)
            .putBoolean(KEY_OWNER_AUTHENTICATED, settings.ownerAuthenticated)
            .apply()

        _settingsFlow.value = settings
    }

    /**
     * Updates an individual setting by key name or property name and emits the change.
     */
    fun updateSetting(key: String, value: Any) {
        val normKey = normalizeKey(key)
        val editor = prefs.edit()
        when (value) {
            is String -> editor.putString(normKey, value)
            is Boolean -> editor.putBoolean(normKey, value)
            is Int -> editor.putInt(normKey, value)
            is Float -> editor.putFloat(normKey, value)
            is Double -> editor.putFloat(normKey, value.toFloat())
            is Long -> editor.putLong(normKey, value)
            else -> editor.putString(normKey, value.toString())
        }
        editor.apply()

        if (normKey == KEY_AI_API_KEY && value is String) {
            try {
                SecureStorage.saveApiKey(value)
            } catch (_: Throwable) {
                // Ignore fallback
            }
        }

        _settingsFlow.value = loadSettings()
    }

    /**
     * Resets all settings to their default initial values.
     */
    fun resetToDefaults() {
        saveSettings(JarvisSettings())
    }

    private fun normalizeKey(key: String): String = when (key) {
        "aiProviderName", KEY_AI_PROVIDER_NAME -> KEY_AI_PROVIDER_NAME
        "aiApiKey", KEY_AI_API_KEY -> KEY_AI_API_KEY
        "aiModelName", KEY_AI_MODEL_NAME -> KEY_AI_MODEL_NAME
        "voiceLanguage", KEY_VOICE_LANGUAGE -> KEY_VOICE_LANGUAGE
        "ttsSpeed", KEY_TTS_SPEED -> KEY_TTS_SPEED
        "ttsEnabled", KEY_TTS_ENABLED -> KEY_TTS_ENABLED
        "proactiveModeEnabled", KEY_PROACTIVE_MODE_ENABLED -> KEY_PROACTIVE_MODE_ENABLED
        "quietModeEnabled", KEY_QUIET_MODE_ENABLED -> KEY_QUIET_MODE_ENABLED
        "quietModeStart", KEY_QUIET_MODE_START -> KEY_QUIET_MODE_START
        "quietModeEnd", KEY_QUIET_MODE_END -> KEY_QUIET_MODE_END
        "maxActionsPerTask", KEY_MAX_ACTIONS_PER_TASK -> KEY_MAX_ACTIONS_PER_TASK
        "taskTimeoutSeconds", KEY_TASK_TIMEOUT_SECONDS -> KEY_TASK_TIMEOUT_SECONDS
        "requireAuthForSensitive", KEY_REQUIRE_AUTH_FOR_SENSITIVE -> KEY_REQUIRE_AUTH_FOR_SENSITIVE
        "autoSendMessages", KEY_AUTO_SEND_MESSAGES -> KEY_AUTO_SEND_MESSAGES
        "ownerAuthenticated", KEY_OWNER_AUTHENTICATED -> KEY_OWNER_AUTHENTICATED
        else -> key
    }

    companion object {
        const val PREFS_NAME = "jarvis_settings_prefs"

        const val KEY_IS_SETUP_COMPLETE = "is_setup_complete"
        const val KEY_USER_NAME = "user_name"
        const val KEY_FULL_NAME = "full_name"
        const val KEY_ASSISTANT_NAME = "assistant_name"
        const val KEY_AI_PROVIDER_NAME = "ai_provider_name"
        const val KEY_AI_API_KEY = "ai_api_key"
        const val KEY_AI_MODEL_NAME = "ai_model_name"
        const val KEY_OPENAI_API_KEY = "openai_api_key"
        const val KEY_SELECTED_VOICE = "selected_voice"
        const val KEY_VOICE_LANGUAGE = "voice_language"
        const val KEY_TTS_SPEED = "tts_speed"
        const val KEY_TTS_ENABLED = "tts_enabled"
        const val KEY_PROACTIVE_MODE_ENABLED = "proactive_mode_enabled"
        const val KEY_QUIET_MODE_ENABLED = "quiet_mode_enabled"
        const val KEY_QUIET_MODE_START = "quiet_mode_start"
        const val KEY_QUIET_MODE_END = "quiet_mode_end"
        const val KEY_MAX_ACTIONS_PER_TASK = "max_actions_per_task"
        const val KEY_TASK_TIMEOUT_SECONDS = "task_timeout_seconds"
        const val KEY_REQUIRE_AUTH_FOR_SENSITIVE = "require_auth_for_sensitive"
        const val KEY_AUTO_SEND_MESSAGES = "auto_send_messages"
        const val KEY_OWNER_AUTHENTICATED = "owner_authenticated"
    }
}
