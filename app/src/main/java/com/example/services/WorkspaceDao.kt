package com.example.services

import androidx.room.*
import com.example.models.WorkspaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {
    @Query("SELECT * FROM workspaces ORDER BY createdAt DESC")
    fun getAllWorkspaces(): Flow<List<WorkspaceEntity>>

    @Query("SELECT * FROM workspaces WHERE isActive = 1 LIMIT 1")
    fun getActiveWorkspace(): Flow<WorkspaceEntity?>

    @Query("SELECT * FROM workspaces WHERE id = :id")
    suspend fun getWorkspaceById(id: String): WorkspaceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspace(workspace: WorkspaceEntity)

    @Update
    suspend fun updateWorkspace(workspace: WorkspaceEntity)

    @Query("DELETE FROM workspaces WHERE id = :id")
    suspend fun deleteWorkspaceById(id: String)

    @Query("UPDATE workspaces SET isActive = 0")
    suspend fun clearActiveWorkspace()

    @Transaction
    suspend fun setActiveWorkspaceTransaction(activeId: String) {
        clearActiveWorkspace()
        setActiveById(activeId)
    }

    @Query("UPDATE workspaces SET isActive = 1 WHERE id = :id")
    suspend fun setActiveById(id: String)
}
