package com.example.services

import com.example.models.WorkspaceEntity
import kotlinx.coroutines.flow.Flow

class WorkspaceRepository(private val workspaceDao: WorkspaceDao) {

    val allWorkspaces: Flow<List<WorkspaceEntity>> = workspaceDao.getAllWorkspaces()
    val activeWorkspace: Flow<WorkspaceEntity?> = workspaceDao.getActiveWorkspace()

    suspend fun getWorkspaceById(id: String): WorkspaceEntity? {
        return workspaceDao.getWorkspaceById(id)
    }

    suspend fun saveWorkspace(workspace: WorkspaceEntity) {
        workspaceDao.insertWorkspace(workspace)
    }

    suspend fun updateWorkspace(workspace: WorkspaceEntity) {
        workspaceDao.updateWorkspace(workspace)
    }

    suspend fun deleteWorkspace(id: String) {
        workspaceDao.deleteWorkspaceById(id)
    }

    suspend fun setActiveWorkspace(id: String) {
        workspaceDao.setActiveWorkspaceTransaction(id)
    }

    suspend fun clearActiveWorkspace() {
        workspaceDao.clearActiveWorkspace()
    }
}
