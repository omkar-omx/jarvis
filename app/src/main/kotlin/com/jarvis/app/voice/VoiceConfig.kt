package com.jarvis.app.voice

/**
 * Configuration options for the Voice Engine (STT and TTS).
 * Configured specifically for Indian English & Hinglish pronunciation with a calm, authoritative OmX AI tone.
 *
 * @property language BCP 47 language tag (e.g., "en-IN" for Indian English & Hinglish, "hi-IN" for Hindi).
 * @property ttsSpeed Speech rate for text-to-speech (1.0f = normal).
 * @property ttsPitch Voice pitch for text-to-speech (0.92f = calm, deep AI tone).
 * @property ttsEnabled Whether voice speech output is enabled.
 * @property continuousListening Whether speech recognition should automatically restart after a recognition cycle.
 */
data class VoiceConfig(
    val language: String = "en-IN",
    val ttsSpeed: Float = 1.02f,
    val ttsPitch: Float = 0.92f,
    val ttsEnabled: Boolean = true,
    val continuousListening: Boolean = false
)
