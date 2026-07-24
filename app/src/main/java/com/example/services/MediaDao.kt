package com.example.services

import androidx.room.*
import com.example.models.MediaItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items WHERE eventId = :eventId ORDER BY createdAt DESC")
    fun getMediaForEvent(eventId: String): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE eventId = :eventId ORDER BY createdAt DESC")
    suspend fun getMediaForEventSync(eventId: String): List<MediaItemEntity>

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getMediaById(id: String): MediaItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(mediaItem: MediaItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaItems(mediaItems: List<MediaItemEntity>)

    @Update
    suspend fun updateMedia(mediaItem: MediaItemEntity)

    @Delete
    suspend fun deleteMedia(mediaItem: MediaItemEntity)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteMediaById(id: String)

    @Query("DELETE FROM media_items WHERE eventId = :eventId")
    suspend fun deleteMediaForEvent(eventId: String)
}
