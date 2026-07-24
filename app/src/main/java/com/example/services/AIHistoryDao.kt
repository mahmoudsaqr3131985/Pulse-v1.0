package com.example.services

import androidx.room.*
import com.example.models.AIRequestHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AIHistoryDao {
    @Query("SELECT * FROM ai_request_history WHERE workspaceId = :workspaceId ORDER BY date DESC")
    fun getHistoryForWorkspace(workspaceId: String): Flow<List<AIRequestHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: AIRequestHistoryEntity)

    @Query("DELETE FROM ai_request_history WHERE workspaceId = :workspaceId")
    suspend fun clearHistoryForWorkspace(workspaceId: String)
}
