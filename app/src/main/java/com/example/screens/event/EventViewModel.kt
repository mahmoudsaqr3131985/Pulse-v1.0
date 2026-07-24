package com.example.screens.event

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.example.models.EventEntity
import com.example.models.MediaItemEntity
import com.example.models.WorkspaceEntity
import com.example.services.AppDatabase
import com.example.services.EventRepository
import com.example.services.MediaRepository
import com.example.services.WorkspaceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class EventViewModel(application: Application) : AndroidViewModel(application) {

    private val eventRepository: EventRepository
    private val workspaceRepository: WorkspaceRepository
    private val mediaRepository: MediaRepository
    private val uploadEngine: com.example.services.GoogleDriveUploadEngine
    private val packageEngine: com.example.services.EventPackageEngine

    val activeWorkspace: StateFlow<WorkspaceEntity?>
    val events: StateFlow<List<EventEntity>>
    val uploadState: StateFlow<com.example.services.UploadProgressState>

    init {
        val database = AppDatabase.getDatabase(application)
        eventRepository = EventRepository(database.eventDao())
        workspaceRepository = WorkspaceRepository(database.workspaceDao())
        mediaRepository = MediaRepository(application, database.mediaDao(), database.eventDao())
        uploadEngine = com.example.services.GoogleDriveUploadEngine.getInstance(
            application,
            database.eventDao(),
            database.mediaDao(),
            database.workspaceDao()
        )
        packageEngine = com.example.services.EventPackageEngine.getInstance(
            application,
            database.eventDao(),
            database.workspaceDao(),
            database.mediaDao()
        )

        uploadState = uploadEngine.uploadState

        activeWorkspace = workspaceRepository.activeWorkspace.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        events = activeWorkspace
            .flatMapLatest { workspace ->
                if (workspace != null) {
                    eventRepository.getEventsForWorkspace(workspace.id)
                } else {
                    flowOf(emptyList())
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    fun getMediaForEvent(eventId: String): Flow<List<MediaItemEntity>> {
        return mediaRepository.getMediaForEvent(eventId)
    }

    fun addCapturedMedia(
        eventId: String,
        workspaceId: String,
        workspaceName: String,
        eventTitle: String,
        tempFile: File,
        fileType: String,
        mimeType: String,
        onComplete: (MediaItemEntity) -> Unit
    ) {
        viewModelScope.launch {
            val result = mediaRepository.addCapturedMedia(
                eventId = eventId,
                workspaceId = workspaceId,
                workspaceName = workspaceName,
                eventTitle = eventTitle,
                tempFile = tempFile,
                fileType = fileType,
                mimeType = mimeType
            )
            onComplete(result)
        }
    }

    fun addMediaFromUris(
        eventId: String,
        workspaceId: String,
        workspaceName: String,
        eventTitle: String,
        uris: List<Uri>,
        onComplete: (List<MediaItemEntity>) -> Unit
    ) {
        viewModelScope.launch {
            val results = mediaRepository.addMediaFromUris(
                eventId = eventId,
                workspaceId = workspaceId,
                workspaceName = workspaceName,
                eventTitle = eventTitle,
                uris = uris
            )
            onComplete(results)
        }
    }

    fun updateMedia(mediaItem: MediaItemEntity, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            mediaRepository.updateMedia(mediaItem)
            onComplete?.invoke()
        }
    }

    fun deleteMedia(mediaItem: MediaItemEntity, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            mediaRepository.deleteMedia(mediaItem)
            onComplete?.invoke()
        }
    }

    suspend fun getEventById(id: String): EventEntity? {
        return eventRepository.getEventById(id)
    }

    fun saveEvent(
        id: String? = null,
        workspaceId: String,
        title: String,
        type: String,
        date: String,
        dateTimestamp: Long,
        time: String? = null,
        location: String? = null,
        description: String? = null,
        leader1Present: Boolean = false,
        leader2Present: Boolean = false,
        guestsJson: String = "[]",
        status: String,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val isEditing = !id.isNullOrBlank()
            val existing = if (isEditing) eventRepository.getEventById(id!!) else null

            val eventToSave = EventEntity(
                id = id ?: java.util.UUID.randomUUID().toString(),
                workspaceId = workspaceId,
                title = title.trim(),
                type = type.trim(),
                date = date,
                dateTimestamp = dateTimestamp,
                time = time?.trim().takeIf { !it.isNullOrBlank() },
                location = location?.trim().takeIf { !it.isNullOrBlank() },
                description = description?.trim().takeIf { !it.isNullOrBlank() },
                leader1Present = leader1Present,
                leader2Present = leader2Present,
                guestsJson = guestsJson,
                status = status,
                photoCount = existing?.photoCount ?: 0,
                videoCount = existing?.videoCount ?: 0,
                documentCount = existing?.documentCount ?: 0,
                audioCount = existing?.audioCount ?: 0,
                generatedPost = existing?.generatedPost,
                generatedHashtags = existing?.generatedHashtags,
                uploadedToDrive = existing?.uploadedToDrive ?: false,
                publishingReady = existing?.publishingReady ?: false,
                publishingScore = existing?.publishingScore ?: 0,
                lastValidationTime = existing?.lastValidationTime ?: 0L,
                publishStatus = existing?.publishStatus ?: com.example.models.PUBLISH_STATUS_DRAFT,
                lastModified = System.currentTimeMillis(),
                createdAt = existing?.createdAt ?: System.currentTimeMillis()
            )

            eventRepository.saveEvent(eventToSave)
            onComplete()
        }
    }

    fun updatePublishingStatus(
        eventId: String,
        ready: Boolean,
        score: Int,
        publishStatus: String,
        onComplete: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            val existing = eventRepository.getEventById(eventId)
            if (existing != null) {
                val updated = existing.copy(
                    publishingReady = ready,
                    publishingScore = score,
                    lastValidationTime = System.currentTimeMillis(),
                    publishStatus = publishStatus,
                    lastModified = System.currentTimeMillis()
                )
                eventRepository.saveEvent(updated)
                onComplete?.invoke()
            }
        }
    }

    fun startOrResumeUpload(eventId: String) {
        viewModelScope.launch {
            uploadEngine.startOrResumeUpload(eventId)
        }
    }

    fun pauseUpload() {
        uploadEngine.pauseUpload()
    }

    fun generateEventPackage(eventId: String, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val result = packageEngine.generatePackage(eventId)
            onResult?.invoke(result.isSuccess)
        }
    }

    fun deleteEvent(event: EventEntity, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            eventRepository.deleteEvent(event)
            onComplete?.invoke()
        }
    }

    fun deleteEventById(id: String, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            eventRepository.deleteEventById(id)
            onComplete?.invoke()
        }
    }
}
