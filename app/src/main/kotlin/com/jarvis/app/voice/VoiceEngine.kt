package com.jarvis.app.voice

import android.content.Context

/**
 * Unified Voice Engine interface for JARVIS voice interaction,
 * providing both speech recognition (STT) and speech synthesis (TTS).
 */
interface VoiceEngine {

    /**
     * Initializes the voice engine components with the provided Android context and configuration.
     *
     * @param context Application or activity context.
     * @param config Initial voice configuration settings.
     */
    fun initialize(context: Context, config: VoiceConfig = VoiceConfig())

    /**
     * Starts listening for user speech input.
     *
     * @param onResult Callback invoked when speech is recognized with the transcribed text.
     * @param onError Callback invoked when recognition fails with an error message.
     */
    fun startListening(onResult: (String) -> Unit, onError: (String) -> Unit)

    /**
     * Stops listening for speech input.
     */
    fun stopListening()

    /**
     * Speaks the provided text using Text-to-Speech synthesis.
     *
     * @param text The text to synthesize into speech.
     * @param onComplete Optional callback invoked when speech synthesis completes or errors out.
     */
    fun speak(text: String, onComplete: (() -> Unit)? = null)

    /**
     * Stops any ongoing speech synthesis immediately.
     */
    fun stopSpeaking()

    /**
     * Updates the language for both recognition and speech synthesis (e.g., "en-US", "hi-IN", "en-IN").
     *
     * @param languageCode BCP 47 language tag or locale code.
     */
    fun setLanguage(languageCode: String)

    /**
     * Sets the speech rate for TTS synthesis.
     *
     * @param speed Playback rate multiplier (1.0f = normal speed).
     */
    fun setSpeed(speed: Float)

    /**
     * Returns true if speech recognition is currently active and listening.
     */
    fun isListening(): Boolean

    /**
     * Returns true if Text-to-Speech is currently speaking audio.
     */
    fun isSpeaking(): Boolean

    /**
     * Releases all resources, destroys recognizers, and terminates TTS engines.
     */
    fun shutdown()
}
