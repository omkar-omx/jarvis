package com.jarvis.app.memory

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jarvis.app.memory.entities.ConversationEntity
import com.jarvis.app.memory.entities.MemoryEntity
import com.jarvis.app.memory.entities.ScheduleEntity
import com.jarvis.app.memory.entities.TaskHistoryEntity
import com.jarvis.app.memory.entities.UserPreferenceEntity

@Database(
    entities = [
        ConversationEntity::class,
        MemoryEntity::class,
        UserPreferenceEntity::class,
        ScheduleEntity::class,
        TaskHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MemoryDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: MemoryDatabase? = null

        fun getInstance(context: Context): MemoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MemoryDatabase::class.java,
                    "jarvis_memory.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
