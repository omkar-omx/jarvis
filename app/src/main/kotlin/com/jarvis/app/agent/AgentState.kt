package com.jarvis.app.agent

/**
 * Represents the execution state of the JARVIS agent engine and tasks.
 */
enum class AgentState {
    IDLE,
    PLANNING,
    EXECUTING,
    WAITING_CONFIRMATION,
    OBSERVING,
    COMPLETED,
    FAILED,
    EMERGENCY_STOPPED;

    fun isTerminal(): Boolean = this in listOf(COMPLETED, FAILED, EMERGENCY_STOPPED)
    fun isActive(): Boolean = this in listOf(PLANNING, EXECUTING, OBSERVING)
    
    fun displayName(): String = when (this) {
        IDLE -> "Idle"
        PLANNING -> "Planning..."
        EXECUTING -> "Executing..."
        WAITING_CONFIRMATION -> "Waiting for confirmation"
        OBSERVING -> "Observing..."
        COMPLETED -> "Completed"
        FAILED -> "Failed"
        EMERGENCY_STOPPED -> "Emergency Stopped"
    }
}
