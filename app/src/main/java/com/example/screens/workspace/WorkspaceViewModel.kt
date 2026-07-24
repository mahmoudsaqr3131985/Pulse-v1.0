package com.example.screens.workspace

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.models.WorkspaceEntity
import com.example.services.AppDatabase
import com.example.services.WorkspaceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkspaceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WorkspaceRepository
    val authManager = com.example.services.GoogleAuthManager.getInstance(application)
    val driveManager = com.example.services.GoogleDriveManager()

    val allWorkspaces: StateFlow<List<WorkspaceEntity>>
    val activeWorkspace: StateFlow<WorkspaceEntity?>

    init {
        val workspaceDao = AppDatabase.getDatabase(application).workspaceDao()
        repository = WorkspaceRepository(workspaceDao)

        allWorkspaces = repository.allWorkspaces.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        activeWorkspace = repository.activeWorkspace.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    }

    suspend fun getWorkspaceById(id: String): WorkspaceEntity? {
        return repository.getWorkspaceById(id)
    }

    fun saveWorkspace(
        id: String? = null,
        name: String,
        type: String,
        leader1Title: String,
        leader1Name: String,
        leader2Title: String?,
        leader2Name: String?,
        defaultHashtags: String,
        isAutoActiveIfFirst: Boolean = true,
        aiProvider: String = com.example.models.AI_PROVIDER_NONE,
        aiApiKeyRaw: String? = null,
        aiModel: String = com.example.models.AI_MODEL_NONE,
        aiTemperature: Float = 0.4f,
        aiMaxTokens: Int = 2048,
        aiLanguage: String = com.example.models.AI_LANGUAGE_AUTO,
        aiConnectionStatus: String = com.example.models.AI_STATUS_NOT_CONFIGURED,
        aiLastValidationTime: Long = 0L,
        onComplete: (savedWorkspaceId: String) -> Unit
    ) {
        viewModelScope.launch {
            val isEditing = !id.isNullOrBlank()
            val existing = if (isEditing) repository.getWorkspaceById(id!!) else null

            val isFirstWorkspace = allWorkspaces.value.isEmpty()
            val makeActive = existing?.isActive ?: (isAutoActiveIfFirst && isFirstWorkspace)
            val generatedId = id ?: java.util.UUID.randomUUID().toString()

            val encryptedApiKey = if (!aiApiKeyRaw.isNullOrEmpty()) {
                com.example.utils.EncryptionUtils.encrypt(aiApiKeyRaw)
            } else {
                existing?.aiApiKey
            }

            val workspace = WorkspaceEntity(
                id = generatedId,
                name = name.trim(),
                type = type.trim(),
                leader1Title = leader1Title.trim(),
                leader1Name = leader1Name.trim(),
                leader2Title = leader2Title?.trim().takeIf { !it.isNullOrBlank() },
                leader2Name = leader2Name?.trim().takeIf { !it.isNullOrBlank() },
                defaultHashtags = defaultHashtags.trim(),
                isActive = makeActive,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                storageType = existing?.storageType ?: com.example.models.STORAGE_TYPE_UNCONFIGURED,
                storageStatus = existing?.storageStatus ?: com.example.models.STORAGE_STATUS_NOT_CONFIGURED,
                driveFolderChoice = existing?.driveFolderChoice,
                driveFolderId = existing?.driveFolderId,
                driveFolderName = existing?.driveFolderName,
                localFolderName = existing?.localFolderName,
                driveConnectionPending = existing?.driveConnectionPending ?: false,
                googleAccountEmail = existing?.googleAccountEmail,
                lastConnectionTime = existing?.lastConnectionTime,
                aiProvider = aiProvider,
                aiApiKey = encryptedApiKey,
                aiModel = aiModel,
                aiTemperature = aiTemperature,
                aiMaxTokens = aiMaxTokens,
                aiLanguage = aiLanguage,
                aiConnectionStatus = if (aiProvider == com.example.models.AI_PROVIDER_NONE) com.example.models.AI_STATUS_NOT_CONFIGURED else aiConnectionStatus,
                aiLastValidationTime = aiLastValidationTime
            )

            repository.saveWorkspace(workspace)

            if (makeActive) {
                repository.setActiveWorkspace(workspace.id)
            }

            onComplete(generatedId)
        }
    }

    fun validateAIConnection(
        workspaceId: String,
        providerName: String,
        rawApiKey: String,
        modelName: String,
        temperature: Float,
        maxTokens: Int,
        language: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val provider = com.example.services.ai.AIAdapterFactory.getProvider(providerName)
            val result = provider.validateConnection(rawApiKey, modelName)
            val duration = System.currentTimeMillis() - startTime
            val success = result.isSuccess
            val statusText = if (success) com.example.models.AI_STATUS_CONNECTED else com.example.models.AI_STATUS_FAILED

            // Record History
            val historyDao = AppDatabase.getDatabase(getApplication()).aiHistoryDao()
            historyDao.insertHistory(
                com.example.models.AIRequestHistoryEntity(
                    workspaceId = workspaceId,
                    date = System.currentTimeMillis(),
                    provider = providerName,
                    model = modelName,
                    operation = "Validate Connection",
                    durationMs = duration,
                    status = if (success) "Success" else "Failed"
                )
            )

            val existing = repository.getWorkspaceById(workspaceId)
            if (existing != null) {
                val encryptedKey = if (rawApiKey.isNotBlank()) com.example.utils.EncryptionUtils.encrypt(rawApiKey) else existing.aiApiKey
                val updated = existing.copy(
                    aiProvider = providerName,
                    aiApiKey = encryptedKey,
                    aiModel = modelName,
                    aiTemperature = temperature,
                    aiMaxTokens = maxTokens,
                    aiLanguage = language,
                    aiConnectionStatus = statusText,
                    aiLastValidationTime = System.currentTimeMillis()
                )
                repository.updateWorkspace(updated)
            }

            onResult(
                success,
                if (success) "AI Connection validated successfully!" else (result.exceptionOrNull()?.message ?: "Validation failed.")
            )
        }
    }

    fun getDecryptedApiKey(encryptedKey: String?): String {
        return com.example.utils.EncryptionUtils.decrypt(encryptedKey) ?: ""
    }

    fun updateStorageConfig(
        workspaceId: String,
        storageType: String,
        storageStatus: String,
        driveFolderChoice: String? = null,
        driveFolderId: String? = null,
        driveFolderName: String? = null,
        localFolderName: String? = null,
        googleAccountEmail: String? = null,
        driveConnectionPending: Boolean = false,
        lastConnectionTime: Long? = null,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val existing = repository.getWorkspaceById(workspaceId)
            if (existing != null) {
                val updated = existing.copy(
                    storageType = storageType,
                    storageStatus = storageStatus,
                    driveFolderChoice = driveFolderChoice,
                    driveFolderId = driveFolderId ?: existing.driveFolderId,
                    driveFolderName = driveFolderName ?: existing.driveFolderName,
                    localFolderName = localFolderName ?: existing.localFolderName,
                    googleAccountEmail = googleAccountEmail ?: existing.googleAccountEmail,
                    driveConnectionPending = driveConnectionPending,
                    lastConnectionTime = lastConnectionTime ?: existing.lastConnectionTime
                )
                repository.updateWorkspace(updated)
            }
            onComplete()
        }
    }

    fun connectGoogleDrive(
        workspaceId: String,
        googleEmail: String,
        driveChoice: String,
        customFolderId: String? = null,
        customFolderName: String? = null,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val workspace = repository.getWorkspaceById(workspaceId)
            if (workspace != null) {
                authManager.signInWithGoogle(googleEmail)

                var finalFolderId = customFolderId
                var finalFolderName = customFolderName

                if (driveChoice == com.example.models.DRIVE_CHOICE_CREATE_NEW) {
                    val createResult = driveManager.createWorkspaceFolder(workspace.name)
                    if (createResult.isSuccess) {
                        val folderInfo = createResult.getOrThrow()
                        finalFolderId = folderInfo.folderId
                        finalFolderName = folderInfo.fullPath
                    }
                } else if (driveChoice == com.example.models.DRIVE_CHOICE_CHOOSE_EXISTING && finalFolderId == null) {
                    val existingFolders = driveManager.listExistingDriveFolders().getOrDefault(emptyList())
                    if (existingFolders.isNotEmpty()) {
                        finalFolderId = existingFolders.first().folderId
                        finalFolderName = existingFolders.first().folderName
                    }
                }

                val isSkip = driveChoice == com.example.models.DRIVE_CHOICE_SKIP
                val status = if (isSkip) com.example.models.STORAGE_STATUS_NOT_CONFIGURED else com.example.models.STORAGE_STATUS_CONNECTED

                val updated = workspace.copy(
                    storageType = com.example.models.STORAGE_TYPE_GOOGLE_DRIVE,
                    storageStatus = status,
                    driveFolderChoice = driveChoice,
                    driveFolderId = if (isSkip) null else finalFolderId,
                    driveFolderName = if (isSkip) null else finalFolderName,
                    googleAccountEmail = googleEmail,
                    driveConnectionPending = false,
                    lastConnectionTime = if (isSkip) null else System.currentTimeMillis()
                )
                repository.updateWorkspace(updated)
            }
            onComplete()
        }
    }

    fun disconnectGoogleDrive(
        workspaceId: String,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val workspace = repository.getWorkspaceById(workspaceId)
            if (workspace != null) {
                val updated = workspace.copy(
                    storageType = com.example.models.STORAGE_TYPE_UNCONFIGURED,
                    storageStatus = com.example.models.STORAGE_STATUS_NOT_CONFIGURED,
                    driveFolderChoice = null,
                    driveFolderId = null,
                    driveFolderName = null,
                    googleAccountEmail = null,
                    driveConnectionPending = false,
                    lastConnectionTime = null
                )
                repository.updateWorkspace(updated)
            }
            onComplete()
        }
    }

    fun setActiveWorkspace(id: String) {
        viewModelScope.launch {
            repository.setActiveWorkspace(id)
        }
    }

    fun deleteWorkspace(id: String, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            val workspaceToDelete = repository.getWorkspaceById(id)
            val wasActive = workspaceToDelete?.isActive == true

            repository.deleteWorkspace(id)

            if (wasActive) {
                // If deleted workspace was active, set the first remaining workspace as active if available
                val remaining = allWorkspaces.value.filter { it.id != id }
                if (remaining.isNotEmpty()) {
                    repository.setActiveWorkspace(remaining.first().id)
                } else {
                    repository.clearActiveWorkspace()
                }
            }

            onComplete?.invoke()
        }
    }
}
