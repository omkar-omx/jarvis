package com.jarvis.app.memory.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val triggerTimeMillis: Long,
    val repeatInterval: Long = 0, // 0 = one-time
    val type: String = "reminder", // reminder, alarm, task, quiet_mode
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastTriggered: Long? = null,
    val payload: String = "" // JSON for extra data
)
