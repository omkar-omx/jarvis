package com.jarvis.app.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Speech-to-Text engine implementing speech recognition via Android's [SpeechRecognizer].
 *
 * All SpeechRecognizer operations are dispatched to the Android main thread as required
 * by the Android Speech framework. Supports languages such as en-US, hi-IN, and en-IN (Hinglish).
 */
class AndroidSpeechRecognizerEngine(
    context: Context
) : RecognitionListener {

    companion object {
        private const val TAG = "JarvisSpeechRecognizer"
    }

    private val appContext: Context = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private var speechRecognizer: SpeechRecognizer? = null
    private var config: VoiceConfig = VoiceConfig()

    private val _isListening = AtomicBoolean(false)
    val isListening: Boolean get() = _isListening.get()

    private val isDestroyed = AtomicBoolean(false)

    private var onResultCallback: ((String) -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null
    private var onPartialResultCallback: ((String) -> Unit)? = null

    /**
     * Initializes the recognizer with the given configuration.
     *
     * @param config Configuration options including language and continuous listening.
     */
    fun initialize(config: VoiceConfig = VoiceConfig()) {
        this.config = config
        runOnMainThread {
            ensureRecognizerCreated()
        }
    }

    /**
     * Starts listening for voice input.
     *
     * @param onResult Invoked when speech is successfully recognized with the transcribed text.
     * @param onError Invoked with an error message when speech recognition fails.
     * @param onPartialResult Optional callback invoked with partial transcription results.
     */
    fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onPartialResult: ((String) -> Unit)? = null
    ) {
        this.onResultCallback = onResult
        this.onErrorCallback = onError
        this.onPartialResultCallback = onPartialResult

        runOnMainThread {
            if (isDestroyed.get()) {
                onError("Speech recognizer has been destroyed")
                return@runOnMainThread
            }

            if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
                Log.e(TAG, "Speech recognition service is not available on this device")
                onError("Speech recognition service is not available on this device")
                return@runOnMainThread
            }

            try {
                ensureRecognizerCreated()
                val intent = buildRecognizerIntent()
                speechRecognizer?.startListening(intent)
                _isListening.set(true)
                Log.d(TAG, "Speech recognition started in language: ${config.language}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start speech recognition", e)
                _isListening.set(false)
                onError("Failed to start speech recognition: ${e.message}")
            }
        }
    }

    /**
     * Stops listening and requests final transcription results for speech spoken so far.
     */
    fun stopListening() {
        runOnMainThread {
            try {
                if (_isListening.get()) {
                    speechRecognizer?.stopListening()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping speech recognizer", e)
            } finally {
                _isListening.set(false)
            }
        }
    }

    /**
     * Cancels any active recognition session without waiting for results.
     */
    fun cancel() {
        runOnMainThread {
            try {
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling speech recognizer", e)
            } finally {
                _isListening.set(false)
            }
        }
    }

    /**
     * Updates the recognition language code (e.g., "en-US", "hi-IN", "en-IN").
     */
    fun setLanguage(languageCode: String) {
        this.config = this.config.copy(language = languageCode)
    }

    /**
     * Updates full configuration for the engine.
     */
    fun updateConfig(config: VoiceConfig) {
        this.config = config
    }

    /**
     * Releases speech recognition resources and destroys the underlying recognizer.
     */
    fun shutdown() {
        isDestroyed.set(true)
        _isListening.set(false)
        onResultCallback = null
        onErrorCallback = null
        onPartialResultCallback = null

        runOnMainThread {
            try {
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                Log.e(TAG, "Error destroying speech recognizer", e)
            } finally {
                speechRecognizer = null
            }
        }
    }

    private fun ensureRecognizerCreated() {
        if (isDestroyed.get()) return
        if (speechRecognizer == null) {
            if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
                Log.w(TAG, "Speech recognition is not available on this device")
                return
            }
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
                setRecognitionListener(this@AndroidSpeechRecognizerEngine)
            }
        }
    }

    private fun buildRecognizerIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, config.language)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, config.language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
        }
    }

    private fun restartListening() {
        if (isDestroyed.get() || !config.continuousListening || _isListening.get()) return
        try {
            ensureRecognizerCreated()
            val intent = buildRecognizerIntent()
            speechRecognizer?.startListening(intent)
            _isListening.set(true)
            Log.d(TAG, "Continuous listening resumed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restart continuous listening", e)
            _isListening.set(false)
        }
    }

    private fun runOnMainThread(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }

    private fun mapErrorCodeToMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client-side recognition error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient microphone permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network connection error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server recognition error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected"
            else -> "Speech recognition error ($errorCode)"
        }
    }

    // --- RecognitionListener Callbacks ---

    override fun onReadyForSpeech(params: Bundle?) {
        Log.d(TAG, "onReadyForSpeech")
        _isListening.set(true)
    }

    override fun onBeginningOfSpeech() {
        Log.d(TAG, "onBeginningOfSpeech")
    }

    override fun onRmsChanged(rmsdB: Float) {
        // Audio sound level update
    }

    override fun onBufferReceived(buffer: ByteArray?) {
        // Incoming audio buffer
    }

    override fun onEndOfSpeech() {
        Log.d(TAG, "onEndOfSpeech")
        _isListening.set(false)
    }

    override fun onError(error: Int) {
        _isListening.set(false)
        val errorMessage = mapErrorCodeToMessage(error)
        Log.w(TAG, "onError: $errorMessage (code: $error)")

        // Notify client callback
        onErrorCallback?.invoke(errorMessage)

        // If continuous listening is enabled and recognizer is not destroyed, schedule restart
        if (config.continuousListening && !isDestroyed.get()) {
            val delayMs = if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 1000L else 500L
            mainHandler.postDelayed({
                if (config.continuousListening && !isDestroyed.get() && !_isListening.get()) {
                    restartListening()
                }
            }, delayMs)
        }
    }

    override fun onResults(results: Bundle?) {
        _isListening.set(false)
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val recognizedText = matches?.firstOrNull()?.trim()

        if (!recognizedText.isNullOrBlank()) {
            Log.d(TAG, "onResults: $recognizedText")
            onResultCallback?.invoke(recognizedText)
        } else {
            onErrorCallback?.invoke("No speech recognized")
        }

        // Handle continuous listening continuation
        if (config.continuousListening && !isDestroyed.get()) {
            mainHandler.postDelayed({
                if (config.continuousListening && !isDestroyed.get() && !_isListening.get()) {
                    restartListening()
                }
            }, 300L)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val partialText = matches?.firstOrNull()?.trim()
        if (!partialText.isNullOrBlank()) {
            Log.v(TAG, "onPartialResults: $partialText")
            onPartialResultCallback?.invoke(partialText)
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {
        Log.v(TAG, "onEvent: $eventType")
    }
}
