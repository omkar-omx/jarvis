package com.jarvis.app.memory

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jarvis.app.memory.entities.ConversationEntity
import com.jarvis.app.memory.entities.MemoryEntity
import com.jarvis.app.memory.entities.ScheduleEntity
import com.jarvis.app.memory.entities.TaskHistoryEntity
import com.jarvis.app.memory.entities.UserPreferenceEntity

@Dao
interface MemoryDao {
    // Conversations
    @Insert
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Query("SELECT * FROM conversations ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentConversations(limit: Int = 50): List<ConversationEntity>

    @Query("SELECT * FROM conversations WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getConversationsBySession(sessionId: String): List<ConversationEntity>

    @Query("DELETE FROM conversations")
    suspend fun clearConversations()

    @Query("DELETE FROM conversations WHERE timestamp < :before")
    suspend fun deleteConversationsBefore(before: Long)

    // Memories
    @Insert
    suspend fun insertMemory(memory: MemoryEntity): Long

    @Update
    suspend fun updateMemory(memory: MemoryEntity)

    @Delete
    suspend fun deleteMemory(memory: MemoryEntity)

    @Query("SELECT * FROM memories WHERE isActive = 1 ORDER BY importance DESC, updatedAt DESC")
    suspend fun getAllActiveMemories(): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE isActive = 1 AND category = :category ORDER BY importance DESC")
    suspend fun getMemoriesByCategory(category: String): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE isActive = 1 AND (content LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%') ORDER BY importance DESC")
    suspend fun searchMemories(query: String): List<MemoryEntity>

    @Query("UPDATE memories SET isActive = 0 WHERE id = :id")
    suspend fun deactivateMemory(id: Long)

    @Query("DELETE FROM memories")
    suspend fun clearAllMemories()

    @Query("SELECT * FROM memories WHERE id = :id")
    suspend fun getMemoryById(id: Long): MemoryEntity?

    // User Preferences
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setPreference(preference: UserPreferenceEntity)

    @Query("SELECT * FROM user_preferences WHERE key = :key")
    suspend fun getPreference(key: String): UserPreferenceEntity?

    @Query("SELECT * FROM user_preferences")
    suspend fun getAllPreferences(): List<UserPreferenceEntity>

    @Query("DELETE FROM user_preferences WHERE key = :key")
    suspend fun deletePreference(key: String)

    // Schedules
    @Insert
    suspend fun insertSchedule(schedule: ScheduleEntity): Long

    @Update
    suspend fun updateSchedule(schedule: ScheduleEntity)

    @Delete
    suspend fun deleteSchedule(schedule: ScheduleEntity)

    @Query("SELECT * FROM schedules WHERE isActive = 1 ORDER BY triggerTimeMillis ASC")
    suspend fun getActiveSchedules(): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE isActive = 1 AND triggerTimeMillis <= :now")
    suspend fun getDueSchedules(now: Long = System.currentTimeMillis()): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun getScheduleById(id: Long): ScheduleEntity?

    @Query("UPDATE schedules SET isActive = 0 WHERE id = :id")
    suspend fun deactivateSchedule(id: Long)

    // Task History
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskHistory(task: TaskHistoryEntity)

    @Query("SELECT * FROM task_history ORDER BY startedAt DESC LIMIT :limit")
    suspend fun getRecentTasks(limit: Int = 20): List<TaskHistoryEntity>

    @Query("SELECT * FROM task_history WHERE taskId = :taskId")
    suspend fun getTaskById(taskId: String): TaskHistoryEntity?

    @Query("DELETE FROM task_history")
    suspend fun clearTaskHistory()

    // Statistics counts
    @Query("SELECT COUNT(*) FROM memories WHERE isActive = 1")
    suspend fun countActiveMemories(): Int

    @Query("SELECT COUNT(*) FROM conversations")
    suspend fun countConversations(): Int

    @Query("SELECT COUNT(*) FROM task_history")
    suspend fun countTasks(): Int

    @Query("SELECT COUNT(*) FROM schedules WHERE isActive = 1")
    suspend fun countActiveSchedules(): Int
}
