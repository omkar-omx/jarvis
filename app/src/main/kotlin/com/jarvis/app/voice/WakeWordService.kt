package com.jarvis.app.voice

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.jarvis.app.MainActivity
import com.jarvis.app.R
import com.jarvis.app.overlay.JarvisAssistantOverlayService
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Foreground Service running the background "Hey JARVIS" wake-word detector.
 * Uses Android SpeechRecognizer to listen for the hotword — exactly like Google Assistant.
 * Wakes the screen, triggers haptics, and launches the Assistant overlay on detection.
 */
class WakeWordService : Service() {

    companion object {
        private const val TAG = "WakeWordService"
        const val CHANNEL_ID = "jarvis_wake_word_channel"
        const val NOTIFICATION_ID = 2001
        const val ACTION_START = "com.jarvis.app.START_WAKE_WORD"
        const val ACTION_STOP = "com.jarvis.app.STOP_WAKE_WORD"

        // Wake phrases supported
        private val WAKE_PHRASES = listOf(
            "hey jarvis", "hey javis", "hey jarves",
            "ok jarvis", "okay jarvis", "jarvis",
            "हे जार्विस", "जार्विस", "jai jarvis"
        )

        fun start(context: Context) {
            val intent = Intent(context, WakeWordService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, WakeWordService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val isRunning = AtomicBoolean(false)
    private var wakeLock: PowerManager.WakeLock? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopListening()
                stopSelf()
            }
            else -> {
                startForeground(NOTIFICATION_ID, buildNotification())
                startListeningLoop()
            }
        }
        return START_STICKY
    }

    // ─── Core listening loop ──────────────────────────────────────────────────

    private fun startListeningLoop() {
        if (isRunning.getAndSet(true)) return

        // Check mic permission before starting SpeechRecognizer
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "RECORD_AUDIO permission not granted — wake word listener waiting for permission.")
            // Retry after 30s in case user grants permission while app is running
            handler.postDelayed({ startListeningLoop() }, 30_000L)
            isRunning.set(false)
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.w(TAG, "SpeechRecognizer not available on this device.")
            return
        }

        Log.i(TAG, "Hey JARVIS wake-word listener armed and active.")
        armSpeechRecognizer()
    }

    private fun armSpeechRecognizer() {
        handler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            Log.d(TAG, "SpeechRecognizer ready — listening for 'Hey JARVIS'")
                        }
                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onPartialResults(partialResults: Bundle?) {
                            val partial = partialResults
                                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull()?.lowercase() ?: return
                            if (WAKE_PHRASES.any { partial.contains(it) }) {
                                onWakeWordDetected()
                            }
                        }
                        override fun onResults(results: Bundle?) {
                            val text = results
                                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull()?.lowercase() ?: ""
                            if (WAKE_PHRASES.any { text.contains(it) }) {
                                onWakeWordDetected()
                            }
                            // Re-arm the recognizer to keep listening
                            if (isRunning.get()) {
                                handler.postDelayed({ armSpeechRecognizer() }, 800L)
                            }
                        }
                        override fun onEndOfSpeech() {}
                        override fun onError(error: Int) {
                            Log.d(TAG, "SpeechRecognizer error: $error — re-arming in 2s")
                            // Re-arm after short delay on any error (silence, no speech, etc.)
                            if (isRunning.get()) {
                                handler.postDelayed({ armSpeechRecognizer() }, 2000L)
                            }
                        }
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val recognizeIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    // Don't show the recognition dialog (background listening)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
                }
                speechRecognizer?.startListening(recognizeIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to arm SpeechRecognizer", e)
                if (isRunning.get()) {
                    handler.postDelayed({ armSpeechRecognizer() }, 5000L)
                }
            }
        }
    }

    // ─── Wake word detected — exactly like Google Assistant ──────────────────

    fun onWakeWordDetected() {
        Log.i(TAG, "WAKE WORD DETECTED: 'Hey JARVIS' — initiating activation pipeline.")

        // 1. Haptic feedback (exactly like Google Assistant)
        triggerHapticPulse()

        // 2. Wake device screen
        wakeDeviceScreen()

        // 3. Launch floating Assistant Overlay with auto-mic
        try {
            val overlayIntent = Intent(this, JarvisAssistantOverlayService::class.java).apply {
                putExtra("auto_listen", true)
            }
            startService(overlayIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch overlay service", e)
        }

        // 4. Bring MainActivity to front
        val activityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("action_wake_activated", true)
        }
        startActivity(activityIntent)

        // 5. Re-arm the recognizer after a short pause (TTS speaks, then we re-listen)
        if (isRunning.get()) {
            handler.postDelayed({ armSpeechRecognizer() }, 4000L)
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun triggerHapticPulse() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(120)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Haptic pulse failed", e)
        }
    }

    private fun wakeDeviceScreen() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "JARVIS:WakeLockTag"
            ).apply {
                acquire(5000L)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Wake lock acquisition failed", e)
        }
    }

    private fun stopListening() {
        isRunning.set(false)
        handler.removeCallbacksAndMessages(null)
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (_: Exception) {}
        wakeLock?.let { if (it.isHeld) it.release() }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopListening()
        serviceScope.cancel()
    }

    // ─── Notification ─────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "J.A.R.V.I.S. Audio Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors for 'Hey Jarvis' hotword activation"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("J.A.R.V.I.S. listening...")
            .setContentText("Say \"Hey JARVIS\" to activate")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
