package com.jarvis.app.memory.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_history")
data class TaskHistoryEntity(
    @PrimaryKey val taskId: String,
    val goal: String,
    val originalCommand: String,
    val status: String, // completed, failed, cancelled, emergency_stopped
    val result: String? = null,
    val actionCount: Int = 0,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val actionLog: String = "" // JSON array of actions taken
)
