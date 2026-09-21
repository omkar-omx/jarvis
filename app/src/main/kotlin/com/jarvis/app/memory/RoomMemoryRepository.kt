package com.jarvis.app.memory

import com.jarvis.app.memory.entities.ConversationEntity
import com.jarvis.app.memory.entities.MemoryEntity
import com.jarvis.app.memory.entities.ScheduleEntity
import com.jarvis.app.memory.entities.TaskHistoryEntity
import com.jarvis.app.memory.entities.UserPreferenceEntity

class RoomMemoryRepository(
    private val dao: MemoryDao,
    private val memorySearch: MemorySearch = MemorySearch(dao)
) : MemoryRepository {

    constructor(database: MemoryDatabase) : this(database.memoryDao())

    override suspend fun saveConversation(role: String, content: String, sessionId: String) {
        val entity = ConversationEntity(
            role = role,
            content = content,
            timestamp = System.currentTimeMillis(),
            sessionId = sessionId
        )
        dao.insertConversation(entity)
    }

    override suspend fun getRecentConversations(limit: Int): List<ConversationEntity> {
        return dao.getRecentConversations(limit)
    }

    override suspend fun clearConversations() {
        dao.clearConversations()
    }

    override suspend fun remember(
        content: String,
        category: String,
        importance: Int,
        tags: String
    ) {
        val now = System.currentTimeMillis()
        val entity = MemoryEntity(
            content = content,
            category = category,
            importance = importance,
            tags = tags,
            createdAt = now,
            updatedAt = now,
            isActive = true
        )
        dao.insertMemory(entity)
    }

    override suspend fun forget(memoryId: Long) {
        dao.deactivateMemory(memoryId)
    }

    override suspend fun recall(query: String): List<MemoryEntity> {
        return memorySearch.search(query)
    }

    override suspend fun getAllMemories(): List<MemoryEntity> {
        return dao.getAllActiveMemories()
    }

    override suspend fun searchMemories(query: String): List<MemoryEntity> {
        return dao.searchMemories(query)
    }

    override suspend fun clearAllMemories() {
        dao.clearAllMemories()
    }

    override suspend fun setPreference(key: String, value: String, category: String) {
        val entity = UserPreferenceEntity(
            key = key,
            value = value,
            category = category,
            updatedAt = System.currentTimeMillis()
        )
        dao.setPreference(entity)
    }

    override suspend fun getPreference(key: String): String? {
        return dao.getPreference(key)?.value
    }

    override suspend fun saveSchedule(schedule: ScheduleEntity): Long {
        return dao.insertSchedule(schedule)
    }

    override suspend fun getActiveSchedules(): List<ScheduleEntity> {
        return dao.getActiveSchedules()
    }

    override suspend fun getDueSchedules(): List<ScheduleEntity> {
        return dao.getDueSchedules(System.currentTimeMillis())
    }

    override suspend fun deactivateSchedule(id: Long) {
        dao.deactivateSchedule(id)
    }

    override suspend fun saveTaskHistory(task: TaskHistoryEntity) {
        dao.insertTaskHistory(task)
    }

    override suspend fun getRecentTasks(limit: Int): List<TaskHistoryEntity> {
        return dao.getRecentTasks(limit)
    }

    override suspend fun getMemoryStats(): MemoryStats {
        return MemoryStats(
            totalMemories = dao.countActiveMemories(),
            totalConversations = dao.countConversations(),
            totalTasks = dao.countTasks(),
            totalSchedules = dao.countActiveSchedules()
        )
    }
}
