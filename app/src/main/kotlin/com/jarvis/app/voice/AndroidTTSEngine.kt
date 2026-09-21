package com.jarvis.app.voice

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Text-to-Speech engine implementing speech synthesis via Android's [TextToSpeech].
 *
 * Implements [TextToSpeech.OnInitListener] to handle asynchronous engine initialization,
 * queuing utterances requested before TTS is fully ready.
 */
class AndroidTTSEngine(
    context: Context
) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "JarvisTTSEngine"
    }

    private val appContext: Context = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private var tts: TextToSpeech? = null

    private val _isSpeaking = AtomicBoolean(false)
    val isSpeaking: Boolean get() = _isSpeaking.get() || (tts?.isSpeaking == true)

    private val _isInitialized = AtomicBoolean(false)
    val isInitialized: Boolean get() = _isInitialized.get()

    private var currentSpeed: Float = 1.02f
    private var currentPitch: Float = 0.92f
    private var currentLocale: Locale = Locale("en", "IN")

    private val utteranceCallbacks = ConcurrentHashMap<String, () -> Unit>()
    private val pendingSpeakQueue = ConcurrentLinkedQueue<Pair<String, (() -> Unit)?>>()

    init {
        tts = TextToSpeech(appContext, this)
    }

    /**
     * Updates engine configuration with the provided [VoiceConfig].
     */
    fun initialize(config: VoiceConfig) {
        this.currentSpeed = config.ttsSpeed
        this.currentPitch = config.ttsPitch
        this.currentLocale = parseLocale(config.language)

        if (_isInitialized.get()) {
            tts?.setSpeechRate(currentSpeed)
            tts?.setPitch(currentPitch)
            applyLocale(currentLocale)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            _isInitialized.set(true)
            setupUtteranceListener()
            tts?.setSpeechRate(currentSpeed)
            tts?.setPitch(currentPitch)
            applyLocale(currentLocale)
            Log.d(TAG, "TTS engine initialized successfully with locale: $currentLocale")
            drainPendingQueue()
        } else {
            _isInitialized.set(false)
            Log.e(TAG, "TTS engine initialization failed with error code: $status")
        }
    }

    /**
     * Synthesizes and speaks the given text.
     *
     * @param text The message to speak.
     * @param onComplete Optional callback invoked when speech playback finishes.
     */
    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        if (text.isBlank()) {
            onComplete?.let { mainHandler.post(it) }
            return
        }

        if (!_isInitialized.get()) {
            Log.d(TAG, "TTS engine not ready yet; enqueuing utterance")
            pendingSpeakQueue.add(Pair(text, onComplete))
            return
        }

        val utteranceId = "jarvis_tts_${System.currentTimeMillis()}_${UUID.randomUUID()}"
        if (onComplete != null) {
            utteranceCallbacks[utteranceId] = onComplete
        }

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        if (result == TextToSpeech.SUCCESS) {
            _isSpeaking.set(true)
        } else {
            Log.e(TAG, "TTS speak failed with status code: $result")
            _isSpeaking.set(false)
            utteranceCallbacks.remove(utteranceId)?.let { cb ->
                mainHandler.post(cb)
            }
        }
    }

    /**
     * Stops any currently playing speech synthesis.
     */
    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS playback", e)
        } finally {
            _isSpeaking.set(false)
            pendingSpeakQueue.clear()
            utteranceCallbacks.clear()
        }
    }

    /**
     * Sets the locale for TTS speech output.
     */
    fun setLanguage(locale: Locale): Int {
        this.currentLocale = locale
        return if (_isInitialized.get()) {
            applyLocale(locale)
        } else {
            TextToSpeech.LANG_AVAILABLE
        }
    }

    /**
     * Sets the language for TTS speech output using a language tag string (e.g., "en-US", "hi-IN", "en-IN").
     */
    fun setLanguage(languageCode: String): Int {
        val locale = parseLocale(languageCode)
        return setLanguage(locale)
    }

    /**
     * Sets the playback speech rate multiplier (1.0f = normal speed).
     */
    fun setSpeed(speed: Float) {
        this.currentSpeed = speed
        if (_isInitialized.get()) {
            tts?.setSpeechRate(speed)
        }
    }

    /**
     * Sets the pitch multiplier (1.0f = normal pitch).
     */
    fun setPitch(pitch: Float) {
        this.currentPitch = pitch
        if (_isInitialized.get()) {
            tts?.setPitch(pitch)
        }
    }

    /**
     * Shuts down the TTS engine and cleans up active resources.
     */
    fun shutdown() {
        stop()
        try {
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS engine", e)
        } finally {
            tts = null
            _isInitialized.set(false)
            _isSpeaking.set(false)
        }
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.set(true)
                Log.d(TAG, "TTS utterance started: $utteranceId")
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.set(false)
                Log.d(TAG, "TTS utterance completed: $utteranceId")
                if (utteranceId != null) {
                    utteranceCallbacks.remove(utteranceId)?.let { cb ->
                        mainHandler.post(cb)
                    }
                }
            }

            @Deprecated("Deprecated in Java", ReplaceWith("onError(utteranceId, -1)"))
            override fun onError(utteranceId: String?) {
                _isSpeaking.set(false)
                Log.w(TAG, "TTS utterance error: $utteranceId")
                if (utteranceId != null) {
                    utteranceCallbacks.remove(utteranceId)?.let { cb ->
                        mainHandler.post(cb)
                    }
                }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _isSpeaking.set(false)
                Log.w(TAG, "TTS utterance error (code $errorCode): $utteranceId")
                if (utteranceId != null) {
                    utteranceCallbacks.remove(utteranceId)?.let { cb ->
                        mainHandler.post(cb)
                    }
                }
            }
        })
    }

    private fun applyLocale(locale: Locale): Int {
        var result = tts?.setLanguage(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Language $locale is not supported. Attempting hi-IN / en-GB fallback.")
            result = tts?.setLanguage(Locale("hi", "IN")) ?: TextToSpeech.LANG_NOT_SUPPORTED
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.UK)
            }
        }
        try {
            val voices = tts?.voices
            val preferredVoice = voices?.find { v ->
                (v.locale.country.equals("IN", ignoreCase = true) || v.locale.country.equals("GB", ignoreCase = true)) &&
                (v.name.contains("male", ignoreCase = true) || v.name.contains("in-", ignoreCase = true))
            } ?: voices?.find { it.locale.country.equals("IN", ignoreCase = true) }

            preferredVoice?.let { tts?.voice = it }
        } catch (_: Exception) {}
        return result
    }

    private fun drainPendingQueue() {
        while (pendingSpeakQueue.isNotEmpty()) {
            val item = pendingSpeakQueue.poll() ?: break
            speak(item.first, item.second)
        }
    }

    private fun parseLocale(languageCode: String): Locale {
        return try {
            val normalized = languageCode.replace('_', '-')
            Locale.forLanguageTag(normalized)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse language tag '$languageCode', falling back to Locale.US", e)
            Locale.US
        }
    }
}
