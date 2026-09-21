package com.jarvis.app.memory

import com.jarvis.app.memory.entities.ConversationEntity
import com.jarvis.app.memory.entities.MemoryEntity
import com.jarvis.app.memory.entities.ScheduleEntity
import com.jarvis.app.memory.entities.TaskHistoryEntity

data class MemoryStats(
    val totalMemories: Int,
    val totalConversations: Int,
    val totalTasks: Int,
    val totalSchedules: Int
)

interface MemoryRepository {
    suspend fun saveConversation(role: String, content: String, sessionId: String = "")
    suspend fun getRecentConversations(limit: Int = 50): List<ConversationEntity>
    suspend fun clearConversations()
    suspend fun remember(content: String, category: String = "general", importance: Int = 5, tags: String = "")
    suspend fun forget(memoryId: Long)
    suspend fun recall(query: String): List<MemoryEntity>
    suspend fun getAllMemories(): List<MemoryEntity>
    suspend fun searchMemories(query: String): List<MemoryEntity>
    suspend fun clearAllMemories()
    suspend fun setPreference(key: String, value: String, category: String = "general")
    suspend fun getPreference(key: String): String?
    suspend fun saveSchedule(schedule: ScheduleEntity): Long
    suspend fun getActiveSchedules(): List<ScheduleEntity>
    suspend fun getDueSchedules(): List<ScheduleEntity>
    suspend fun deactivateSchedule(id: Long)
    suspend fun saveTaskHistory(task: TaskHistoryEntity)
    suspend fun getRecentTasks(limit: Int = 20): List<TaskHistoryEntity>
    suspend fun getMemoryStats(): MemoryStats
}
