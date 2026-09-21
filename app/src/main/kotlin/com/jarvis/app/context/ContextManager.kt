package com.jarvis.app.context

import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for tracking and providing situational context of the device and user.
 */
interface ContextManager {
    val currentContext: StateFlow<JarvisContext>
    suspend fun updateContext()
    suspend fun getRelevantContext(query: String): Map<String, String>
}

/**
 * Encapsulates the current operational state and environment of JARVIS.
 */
data class JarvisContext(
    val currentApp: String? = null,
    val currentActivity: String? = null,
    val timeOfDay: String = "",
    val isQuietMode: Boolean = false,
    val activeSchedules: List<String> = emptyList(),
    val recentActions: List<String> = emptyList(),
    val batteryLevel: Int? = null,
    val isConnected: Boolean = true
)
