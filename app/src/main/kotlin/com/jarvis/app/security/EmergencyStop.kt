package com.jarvis.app.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Global Emergency Stop mechanism for JARVIS.
 *
 * When activated, all autonomous agent actions, speech synthesis, and background tasks
 * are halted immediately. Registered listeners are triggered on activation to abort
 * in-flight operations safely.
 */
object EmergencyStop {
    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    val listeners: MutableList<() -> Unit> = CopyOnWriteArrayList()

    /**
     * Activates emergency stop, setting isActive to true and triggering all listeners.
     */
    fun activate() {
        _isActive.value = true
        for (listener in listeners) {
            try {
                listener.invoke()
            } catch (_: Throwable) {
                // Ignore errors from individual listeners to guarantee all listeners execute
            }
        }
    }

    /**
     * Deactivates emergency stop, restoring normal system state.
     */
    fun deactivate() {
        _isActive.value = false
    }

    /**
     * Resets emergency stop state (identical to deactivate).
     */
    fun reset() {
        deactivate()
    }

    /**
     * Adds a callback listener to be invoked immediately when emergency stop is triggered.
     */
    fun addOnStopListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    /**
     * Removes a previously registered stop listener.
     */
    fun removeOnStopListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    /**
     * Clears all registered stop listeners.
     */
    fun clearListeners() {
        listeners.clear()
    }
}
