package com.jarvis.app

import android.app.Application
import com.jarvis.app.memory.MemoryDatabase
import com.jarvis.app.memory.MemoryRepository
import com.jarvis.app.memory.RoomMemoryRepository
import com.jarvis.app.notifications.NotificationChannels
import com.jarvis.app.permissions.PermissionManager
import com.jarvis.app.security.SecureStorage
import com.jarvis.app.settings.SettingsRepository

/**
 * Main Application class for JARVIS.
 * Initializes core infrastructure: encrypted keystore storage, preferences repository,
 * runtime permissions manager, notification channels, and Room memory database.
 */
class JarvisApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 1. Initialize SecureStorage
        SecureStorage.init(this)

        // 2. Initialize SettingsRepository
        settingsRepository = SettingsRepository(this)

        // 3. Initialize PermissionManager
        permissionManager = PermissionManager(this)

        // 4. Create Room database instance (MemoryDatabase)
        database = MemoryDatabase.getInstance(this)

        // 5. Initialize Notification Channels
        try {
            NotificationChannels.createChannels(this)
        } catch (_: Throwable) {
            // Failsafe in unit-test or headless environments
        }

        // 6. Initialize BrainManager if setup is complete
        val settings = settingsRepository.settingsFlow.value
        if (settings.isSetupComplete && settings.aiApiKey.isNotBlank()) {
            com.jarvis.app.brain.BrainManager.configureProvider(
                com.jarvis.app.brain.AIProviderConfig(
                    providerName = settings.aiProviderName,
                    apiKey = settings.aiApiKey,
                    modelName = settings.aiModelName
                )
            )
        }

        // 7. Initialize WakeWordService (Background "Hey Jarvis" audio listener)
        try {
            if (settings.isSetupComplete) {
                com.jarvis.app.voice.WakeWordService.start(this)
            }
        } catch (e: Exception) {
            android.util.Log.e("JarvisApp", "Failed to start wake word service", e)
        }
    }

    companion object {
        lateinit var instance: JarvisApplication
            private set

        lateinit var database: MemoryDatabase
            internal set

        lateinit var settingsRepository: SettingsRepository
            internal set

        lateinit var permissionManager: PermissionManager
            internal set

        val secureStorage: SecureStorage
            get() = SecureStorage

        val memoryRepository: MemoryRepository by lazy {
            RoomMemoryRepository(database)
        }

        val ttsEngine: com.jarvis.app.voice.AndroidTTSEngine by lazy {
            com.jarvis.app.voice.AndroidTTSEngine(instance).apply {
                initialize(com.jarvis.app.voice.VoiceConfig(language = "en-IN", ttsSpeed = 1.02f, ttsPitch = 0.92f))
            }
        }

        val speechRecognizerEngine: com.jarvis.app.voice.AndroidSpeechRecognizerEngine by lazy {
            com.jarvis.app.voice.AndroidSpeechRecognizerEngine(instance).apply {
                initialize(com.jarvis.app.voice.VoiceConfig(language = "en-IN"))
            }
        }
    }
}
