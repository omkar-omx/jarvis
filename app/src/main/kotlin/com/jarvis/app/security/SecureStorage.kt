package com.jarvis.app.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure storage implementation leveraging AndroidX Security crypto.
 *
 * Utilizes MasterKey with AES256_GCM encryption scheme backed by Android Keystore
 * and EncryptedSharedPreferences for hardware-backed confidentiality of API keys
 * and sensitive user credentials.
 */
object SecureStorage {
    private const val TAG = "SecureStorage"
    private const val SECURE_PREFS_FILE = "jarvis_secure_prefs"
    private const val KEY_API_KEY_GEMINI = "jarvis_gemini_api_key"
    private const val KEY_API_KEY_TAVILY = "jarvis_tavily_api_key"
    private const val KEY_API_KEY_WEATHER = "jarvis_weather_api_key"
    @Volatile
    private var sharedPreferences: SharedPreferences? = null

    /**
     * Initializes SecureStorage using MasterKey with AES256_GCM scheme.
     * Safe to call multiple times; subsequent calls are no-ops once initialized.
     */
    fun init(context: Context) {
        if (sharedPreferences == null) {
            synchronized(this) {
                if (sharedPreferences == null) {
                    val appContext = context.applicationContext
                    sharedPreferences = try {
                        createEncryptedPreferences(appContext)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to initialize EncryptedSharedPreferences, resetting keystore alias", e)
                        try {
                            appContext.deleteSharedPreferences(SECURE_PREFS_FILE)
                            createEncryptedPreferences(appContext)
                        } catch (fallbackEx: Exception) {
                            Log.e(TAG, "Failed second attempt at EncryptedSharedPreferences; falling back to private prefs", fallbackEx)
                            appContext.getSharedPreferences(SECURE_PREFS_FILE, Context.MODE_PRIVATE)
                        }
                    }
                }
            }
        }
    }

    private fun createEncryptedPreferences(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            SECURE_PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun getPrefs(): SharedPreferences {
        return sharedPreferences
            ?: throw IllegalStateException("SecureStorage has not been initialized. Call init(context) first.")
    }

    /**
     * Stores the AI provider API key securely in encrypted storage.
     */
    fun saveApiKey(key: String, type: String = "gemini") {
        val storageKey = when(type) {
            "tavily" -> KEY_API_KEY_TAVILY
            "weather" -> KEY_API_KEY_WEATHER
            else -> KEY_API_KEY_GEMINI
        }
        saveString(storageKey, key)
    }

    /**
     * Retrieves the stored AI provider API key, or null if none is saved.
     */
    fun getApiKey(type: String = "gemini"): String? {
        val storageKey = when(type) {
            "tavily" -> KEY_API_KEY_TAVILY
            "weather" -> KEY_API_KEY_WEATHER
            else -> KEY_API_KEY_GEMINI
        }
        return getString(storageKey)
    }

    /**
     * Removes the stored AI provider API key.
     */
    fun clearApiKey(type: String = "gemini") {
        val storageKey = when(type) {
            "tavily" -> KEY_API_KEY_TAVILY
            "weather" -> KEY_API_KEY_WEATHER
            else -> KEY_API_KEY_GEMINI
        }
        sharedPreferences?.edit()?.remove(storageKey)?.apply()
    }

    /**
     * Persists a generic string key-value pair securely.
     */
    fun saveString(key: String, value: String) {
        getPrefs().edit().putString(key, value).apply()
    }

    /**
     * Retrieves a generic string value by key, or null if not found.
     */
    fun getString(key: String): String? {
        return getPrefs().getString(key, null)
    }

    /**
     * Clears all entries in secure preferences.
     */
    fun clearAll() {
        getPrefs().edit().clear().apply()
    }
}
