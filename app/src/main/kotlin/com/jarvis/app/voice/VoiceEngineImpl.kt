package com.jarvis.app.voice

import android.content.Context
import android.util.Log

/**
 * Unified implementation of [VoiceEngine] that coordinates both [AndroidSpeechRecognizerEngine]
 * and [AndroidTTSEngine].
 *
 * Handles voice arbitration to ensure the assistant does not listen to its own voice output
 * while speaking, and manages configuration updates across both STT and TTS subsystems.
 */
class VoiceEngineImpl(
    context: Context? = null,
    initialConfig: VoiceConfig = VoiceConfig()
) : VoiceEngine {

    companion object {
        private const val TAG = "JarvisVoiceEngine"
    }

    private var currentContext: Context? = context?.applicationContext
    private var config: VoiceConfig = initialConfig

    private var speechRecognizerEngine: AndroidSpeechRecognizerEngine? = null
    private var ttsEngine: AndroidTTSEngine? = null

    private var lastOnResult: ((String) -> Unit)? = null
    private var lastOnError: ((String) -> Unit)? = null

    init {
        context?.let { ctx ->
            initialize(ctx, initialConfig)
        }
    }

    @Synchronized
    override fun initialize(context: Context, config: VoiceConfig) {
        val appContext = context.applicationContext
        this.currentContext = appContext
        this.config = config

        if (speechRecognizerEngine == null) {
            speechRecognizerEngine = AndroidSpeechRecognizerEngine(appContext)
        }
        speechRecognizerEngine?.initialize(config)

        if (ttsEngine == null) {
            ttsEngine = AndroidTTSEngine(appContext)
        }
        ttsEngine?.initialize(config)

        Log.d(TAG, "VoiceEngine initialized with language: ${config.language}, continuous: ${config.continuousListening}")
    }

    override fun startListening(onResult: (String) -> Unit, onError: (String) -> Unit) {
        val context = currentContext
        if (speechRecognizerEngine == null && context != null) {
            initialize(context, config)
        }

        val engine = speechRecognizerEngine
        if (engine == null) {
            onError("VoiceEngine has not been initialized. Call initialize(context, config) first.")
            return
        }

        // Avoid listening to own voice output
        if (isSpeaking()) {
            stopSpeaking()
        }

        this.lastOnResult = onResult
        this.lastOnError = onError

        engine.startListening(
            onResult = { result ->
                onResult(result)
            },
            onError = { error ->
                onError(error)
            }
        )
    }

    override fun stopListening() {
        speechRecognizerEngine?.stopListening()
    }

    override fun speak(text: String, onComplete: (() -> Unit)?) {
        if (!config.ttsEnabled) {
            Log.d(TAG, "TTS output is disabled in config; skipping speech playback")
            onComplete?.invoke()
            return
        }

        val context = currentContext
        if (ttsEngine == null && context != null) {
            initialize(context, config)
        }

        val engine = ttsEngine
        if (engine == null) {
            Log.w(TAG, "Cannot speak: TTS engine has not been initialized")
            onComplete?.invoke()
            return
        }

        // Temporarily pause listening while speaking so JARVIS does not hear itself
        val wasListening = isListening()
        if (wasListening) {
            speechRecognizerEngine?.stopListening()
        }

        engine.speak(text) {
            onComplete?.invoke()

            // Resume continuous listening if it was active before speech output
            if (wasListening && config.continuousListening) {
                val resCb = lastOnResult
                val errCb = lastOnError
                if (resCb != null && errCb != null) {
                    speechRecognizerEngine?.startListening(resCb, errCb)
                }
            }
        }
    }

    override fun stopSpeaking() {
        ttsEngine?.stop()
    }

    override fun setLanguage(languageCode: String) {
        this.config = config.copy(language = languageCode)
        speechRecognizerEngine?.setLanguage(languageCode)
        ttsEngine?.setLanguage(languageCode)
        Log.d(TAG, "Voice language updated to: $languageCode")
    }

    override fun setSpeed(speed: Float) {
        this.config = config.copy(ttsSpeed = speed)
        ttsEngine?.setSpeed(speed)
    }

    /**
     * Sets the pitch multiplier for TTS synthesis.
     */
    fun setPitch(pitch: Float) {
        this.config = config.copy(ttsPitch = pitch)
        ttsEngine?.setPitch(pitch)
    }

    /**
     * Updates full voice engine configuration.
     */
    fun updateConfig(newConfig: VoiceConfig) {
        this.config = newConfig
        speechRecognizerEngine?.updateConfig(newConfig)
        ttsEngine?.initialize(newConfig)
    }

    /**
     * Returns the current [VoiceConfig].
     */
    fun getConfig(): VoiceConfig = config

    override fun isListening(): Boolean {
        return speechRecognizerEngine?.isListening ?: false
    }

    override fun isSpeaking(): Boolean {
        return ttsEngine?.isSpeaking ?: false
    }

    override fun shutdown() {
        speechRecognizerEngine?.shutdown()
        ttsEngine?.shutdown()
        speechRecognizerEngine = null
        ttsEngine = null
        lastOnResult = null
        lastOnError = null
        Log.d(TAG, "VoiceEngine shut down successfully")
    }
}
