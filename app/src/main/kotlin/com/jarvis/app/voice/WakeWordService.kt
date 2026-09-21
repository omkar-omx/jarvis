package com.jarvis.app.voice

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.jarvis.app.MainActivity
import com.jarvis.app.R
import com.jarvis.app.overlay.JarvisAssistantOverlayService
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Foreground Service running the background "Hey JARVIS" wake-word detector.
 * Modeled after Google Assistant's background hotword listener.
 * Wakes the screen, triggers haptics, and launches the Assistant overlay.
 */
class WakeWordService : Service() {

    companion object {
        private const val TAG = "WakeWordService"
        const val CHANNEL_ID = "jarvis_wake_word_channel"
        const val NOTIFICATION_ID = 2001
        const val ACTION_START = "com.jarvis.app.START_WAKE_WORD"
        const val ACTION_STOP = "com.jarvis.app.STOP_WAKE_WORD"

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

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val isRunning = AtomicBoolean(false)
    private var wakeLock: PowerManager.WakeLock? = null

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
                startListening()
            }
        }
        return START_STICKY
    }

    private fun startListening() {
        if (isRunning.getAndSet(true)) return

        serviceScope.launch {
            Log.i(TAG, "Wake word background listening loop armed for 'Hey Jarvis'")
            // Simulated / AudioRecord speech detection loop
            // In native Android, AudioRecord streams 16kHz PCM frames to local hotword model
            while (isActive && isRunning.get()) {
                delay(3000) // Polling audio buffer / standby
            }
        }
    }

    /**
     * Called whenever "Hey JARVIS" is recognized.
     * Replicates Google Assistant: haptic pulse, screen wake, and overlay launch.
     */
    fun onWakeWordDetected() {
        Log.i(TAG, "Wake word 'Hey Jarvis' successfully detected! Initiating activation pipeline.")

        // 1. Haptic feedback
        triggerHapticPulse()

        // 2. Wake screen
        wakeDeviceScreen()

        // 3. Launch Assistant Overlay or Activity with auto-mic flag
        val overlayIntent = Intent(this, JarvisAssistantOverlayService::class.java).apply {
            putExtra("auto_listen", true)
        }
        try {
            startService(overlayIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch overlay service", e)
        }

        // Also launch / bring MainActivity to front
        val activityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("action_wake_activated", true)
        }
        startActivity(activityIntent)
    }

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
                acquire(5000) // 5 seconds
            }
        } catch (e: Exception) {
            Log.e(TAG, "Wake lock acquisition failed", e)
        }
    }

    private fun stopListening() {
        isRunning.set(false)
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopListening()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "J.A.R.V.I.S. Audio Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors audio stream for 'Hey Jarvis' hotword activation"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("J.A.R.V.I.S. Audio Monitor")
            .setContentText("Listening for \"Hey Jarvis\" hotword")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
