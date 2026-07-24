package com.example.services

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.models.AIRequestHistoryEntity
import com.example.models.EventAIContentEntity
import com.example.models.EventEntity
import com.example.models.MediaItemEntity
import com.example.models.WorkspaceEntity

@Database(
    entities = [
        WorkspaceEntity::class,
        EventEntity::class,
        MediaItemEntity::class,
        AIRequestHistoryEntity::class,
        EventAIContentEntity::class
    ],
    version = 11,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workspaceDao(): WorkspaceDao
    abstract fun eventDao(): EventDao
    abstract fun mediaDao(): MediaDao
    abstract fun aiHistoryDao(): AIHistoryDao
    abstract fun eventAIContentDao(): EventAIContentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pulse_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
