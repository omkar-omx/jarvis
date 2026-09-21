package com.jarvis.app.memory.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String, // "user", "jarvis", "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sessionId: String = "",
    val metadata: String = "" // JSON string for extra data
)
