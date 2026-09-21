package com.jarvis.app.accessibility

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton bridge facilitating communication between the JARVIS application logic,
 * agent automation loop, and the background [JarvisAccessibilityService].
 */
object AccessibilityBridge {
    @Volatile
    private var service: JarvisAccessibilityService? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _lastScreenInfo = MutableStateFlow<ScreenInfo?>(null)
    val lastScreenInfo: StateFlow<ScreenInfo?> = _lastScreenInfo.asStateFlow()

    /**
     * Registers an active instance of [JarvisAccessibilityService].
     */
    @Synchronized
    fun registerService(service: JarvisAccessibilityService) {
        this.service = service
        _isConnected.value = true
    }

    /**
     * Unregisters the service when destroyed or disabled.
     */
    @Synchronized
    fun unregisterService() {
        this.service = null
        _isConnected.value = false
        _lastScreenInfo.value = null
    }

    /**
     * Returns the currently active [JarvisAccessibilityService] instance, if connected.
     */
    fun getService(): JarvisAccessibilityService? = service

    /**
     * Updates the latest cached [ScreenInfo] in the state flow.
     */
    fun updateScreenInfo(info: ScreenInfo) {
        _lastScreenInfo.value = info
    }

    /**
     * Requests the accessibility service to capture and return the latest UI hierarchy snapshot.
     */
    suspend fun readCurrentScreen(): ScreenInfo? {
        val info = service?.captureScreenInfo()
        if (info != null) {
            _lastScreenInfo.value = info
        }
        return info
    }

    /**
     * Taps on the element identified by [nodeId].
     */
    suspend fun performTap(nodeId: String): Boolean {
        return service?.performTap(nodeId) ?: false
    }

    /**
     * Long presses on the element identified by [nodeId].
     */
    suspend fun performLongPress(nodeId: String): Boolean {
        return service?.performLongPress(nodeId) ?: false
    }

    /**
     * Swipes between the specified screen coordinates over [durationMs].
     */
    suspend fun performSwipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = 300L
    ): Boolean {
        return service?.performSwipe(startX, startY, endX, endY, durationMs) ?: false
    }

    /**
     * Scrolls the element or active screen in the given [direction] ("UP", "DOWN", "LEFT", "RIGHT").
     */
    suspend fun performScroll(direction: String, nodeId: String? = null): Boolean {
        return service?.performScroll(direction, nodeId) ?: false
    }

    /**
     * Types [text] into the element identified by [nodeId] or the active input focus.
     */
    suspend fun performTypeText(text: String, nodeId: String? = null): Boolean {
        return service?.performTypeText(text, nodeId) ?: false
    }

    /**
     * Triggers the global Back button action.
     */
    suspend fun performBack(): Boolean {
        return service?.performBack() ?: false
    }

    /**
     * Triggers the global Home button action.
     */
    suspend fun performHome(): Boolean {
        return service?.performHome() ?: false
    }

    /**
     * Triggers the global Overview / Recent Apps button action.
     */
    suspend fun performRecents(): Boolean {
        return service?.performRecents() ?: false
    }
}
