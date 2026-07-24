package com.example.services

import androidx.room.*
import com.example.models.EventAIContentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventAIContentDao {
    @Query("SELECT * FROM event_ai_contents WHERE eventId = :eventId ORDER BY generationTime DESC LIMIT 1")
    fun getAIContentForEventFlow(eventId: String): Flow<EventAIContentEntity?>

    @Query("SELECT * FROM event_ai_contents WHERE eventId = :eventId ORDER BY generationTime DESC LIMIT 1")
    suspend fun getAIContentForEvent(eventId: String): EventAIContentEntity?

    @Query("SELECT * FROM event_ai_contents ORDER BY generationTime DESC")
    fun getAllAIContentsFlow(): Flow<List<EventAIContentEntity>>

    @Query("SELECT * FROM event_ai_contents WHERE id = :id")
    suspend fun getAIContentById(id: String): EventAIContentEntity?

    @Query("SELECT * FROM event_ai_contents WHERE id = :id")
    fun getAIContentByIdFlow(id: String): Flow<EventAIContentEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAIContent(aiContent: EventAIContentEntity)

    @Query("DELETE FROM event_ai_contents WHERE id = :id")
    suspend fun deleteAIContentById(id: String)

    @Query("DELETE FROM event_ai_contents WHERE eventId = :eventId")
    suspend fun deleteAIContentForEvent(eventId: String)
}
