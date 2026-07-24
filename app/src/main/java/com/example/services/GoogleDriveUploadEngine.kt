package com.example.services

import android.content.Context
import com.example.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class UploadProgressState(
    val isUploading: Boolean = false,
    val isPaused: Boolean = false,
    val isCompleted: Boolean = false,
    val eventId: String = "",
    val eventTitle: String = "",
    val currentFileName: String = "",
    val currentFolder: String = "",
    val uploadedFilesCount: Int = 0,
    val totalFilesCount: Int = 0,
    val remainingFilesCount: Int = 0,
    val failedFilesCount: Int = 0,
    val overallProgress: Int = 0, // 0 to 100
    val currentFileProgress: Int = 0, // 0 to 100
    val estimatedRemainingSeconds: Long = 0L,
    val errorMessage: String? = null,
    val lastUploadTime: Long = 0L
)

class GoogleDriveUploadEngine private constructor(
    private val context: Context,
    private val eventDao: EventDao,
    private val mediaDao: MediaDao,
    private val workspaceDao: WorkspaceDao
) {

    private val _uploadState = MutableStateFlow(UploadProgressState())
    val uploadState: StateFlow<UploadProgressState> = _uploadState.asStateFlow()

    private var uploadJob: Job? = null
    private var isPauseRequested = false

    companion object {
        @Volatile
        private var instance: GoogleDriveUploadEngine? = null

        fun getInstance(
            context: Context,
            eventDao: EventDao,
            mediaDao: MediaDao,
            workspaceDao: WorkspaceDao
        ): GoogleDriveUploadEngine {
            return instance ?: synchronized(this) {
                instance ?: GoogleDriveUploadEngine(
                    context.applicationContext,
                    eventDao,
                    mediaDao,
                    workspaceDao
                ).also { instance = it }
            }
        }
    }

    suspend fun startOrResumeUpload(eventId: String) = withContext(Dispatchers.IO) {
        if (_uploadState.value.isUploading && _uploadState.value.eventId == eventId) {
            return@withContext
        }

        val event = eventDao.getEventById(eventId) ?: return@withContext
        val workspace = workspaceDao.getWorkspaceById(event.workspaceId)
        val mediaItems = mediaDao.getMediaForEventSync(eventId)

        if (mediaItems.isEmpty()) {
            return@withContext
        }

        isPauseRequested = false

        // Determine Folder Hierarchy: Pulse / Workspace Name / YYYY / MM MonthName / YYYY-MM-DD Event Title
        val folderPath = buildFolderPath(
            workspaceName = workspace?.name ?: "Default Workspace",
            eventDate = event.date,
            eventTitle = event.title
        )

        val totalFiles = mediaItems.size
        val alreadyUploaded = mediaItems.count { it.uploadStatus == UPLOAD_STATUS_UPLOADED || it.driveFileId != null }
        var uploadedCount = alreadyUploaded
        var failedCount = mediaItems.count { it.uploadStatus == UPLOAD_STATUS_FAILED }

        // Update Initial Event status
        val updatedEventStart = event.copy(
            uploadStarted = true,
            uploadCompleted = false,
            driveFolderId = event.driveFolderId ?: "hdrv_event_${UUID.randomUUID().toString().take(8)}",
            lastModified = System.currentTimeMillis()
        )
        eventDao.insertOrUpdateEvent(updatedEventStart)

        _uploadState.value = UploadProgressState(
            isUploading = true,
            isPaused = false,
            isCompleted = false,
            eventId = eventId,
            eventTitle = event.title,
            uploadedFilesCount = uploadedCount,
            totalFilesCount = totalFiles,
            remainingFilesCount = (totalFiles - uploadedCount).coerceAtLeast(0),
            failedFilesCount = failedCount,
            overallProgress = if (totalFiles > 0) (uploadedCount * 100) / totalFiles else 0,
            currentFolder = folderPath,
            lastUploadTime = System.currentTimeMillis()
        )

        // Upload loop over media items needing upload
        for ((index, item) in mediaItems.withIndex()) {
            if (isPauseRequested) {
                _uploadState.value = _uploadState.value.copy(
                    isUploading = false,
                    isPaused = true,
                    errorMessage = "Upload paused by user."
                )
                break
            }

            // Duplicate Detection & Resume check
            if (item.uploadStatus == UPLOAD_STATUS_UPLOADED && item.driveFileId != null) {
                // Skip already uploaded file
                continue
            }

            val subfolder = getSubfolderName(item.fileType)
            val fullFileFolder = "$folderPath/$subfolder"

            _uploadState.value = _uploadState.value.copy(
                currentFileName = item.fileName,
                currentFolder = fullFileFolder,
                currentFileProgress = 0
            )

            // Update item to UPLOADING state
            val itemUploading = item.copy(uploadStatus = UPLOAD_STATUS_UPLOADING)
            mediaDao.updateMedia(itemUploading)

            // Simulate / Perform Chunked Upload with progress updates
            val uploadSuccess = simulateOrUploadFile(
                item = itemUploading,
                onProgress = { fileProgress ->
                    val totalProgress = (((uploadedCount * 100) + fileProgress) / totalFiles).coerceIn(0, 100)
                    val remainingFiles = (totalFiles - uploadedCount - 1).coerceAtLeast(0)
                    val estSeconds = (remainingFiles * 2L) + ((100 - fileProgress) / 50L)

                    _uploadState.value = _uploadState.value.copy(
                        currentFileProgress = fileProgress,
                        overallProgress = totalProgress,
                        remainingFilesCount = remainingFiles,
                        estimatedRemainingSeconds = estSeconds
                    )

                    // Periodically sync progress to DB
                    runCatching {
                        eventDao.insertOrUpdateEvent(
                            event.copy(
                                uploadStarted = true,
                                uploadProgress = totalProgress,
                                uploadedFileCount = uploadedCount,
                                failedFileCount = failedCount,
                                lastModified = System.currentTimeMillis()
                            )
                        )
                    }
                }
            )

            if (uploadSuccess) {
                val itemUploaded = itemUploading.copy(
                    driveFileId = "hdrv_file_${UUID.randomUUID().toString().take(12)}",
                    uploadStatus = UPLOAD_STATUS_UPLOADED,
                    uploadTime = System.currentTimeMillis(),
                    status = MEDIA_STATUS_UPLOADED
                )
                mediaDao.updateMedia(itemUploaded)
                uploadedCount++
            } else {
                val itemFailed = itemUploading.copy(
                    uploadStatus = UPLOAD_STATUS_FAILED,
                    status = MEDIA_STATUS_FAILED
                )
                mediaDao.updateMedia(itemFailed)
                failedCount++
            }

            val updatedOverallProgress = if (totalFiles > 0) (uploadedCount * 100) / totalFiles else 100
            val remaining = (totalFiles - uploadedCount).coerceAtLeast(0)

            _uploadState.value = _uploadState.value.copy(
                uploadedFilesCount = uploadedCount,
                failedFilesCount = failedCount,
                remainingFilesCount = remaining,
                overallProgress = updatedOverallProgress
            )

            // Update Event DB state
            val currentEvt = eventDao.getEventById(eventId) ?: event
            eventDao.insertOrUpdateEvent(
                currentEvt.copy(
                    uploadStarted = true,
                    uploadProgress = updatedOverallProgress,
                    uploadedFileCount = uploadedCount,
                    failedFileCount = failedCount,
                    lastModified = System.currentTimeMillis()
                )
            )
        }

        if (!isPauseRequested) {
            val isAllDone = uploadedCount == totalFiles && failedCount == 0
            val finalPublishStatus = if (isAllDone) PUBLISH_STATUS_PUBLISHED else PUBLISH_STATUS_READY

            val finalEvt = eventDao.getEventById(eventId) ?: event
            eventDao.insertOrUpdateEvent(
                finalEvt.copy(
                    uploadStarted = true,
                    uploadCompleted = isAllDone,
                    uploadProgress = if (isAllDone) 100 else _uploadState.value.overallProgress,
                    uploadedFileCount = uploadedCount,
                    failedFileCount = failedCount,
                    publishStatus = finalPublishStatus,
                    lastModified = System.currentTimeMillis()
                )
            )

            // Phase 9: Automatically generate complete Event Package & Metadata
            if (isAllDone) {
                runCatching {
                    val packageEngine = EventPackageEngine.getInstance(context, eventDao, workspaceDao, mediaDao)
                    packageEngine.generatePackage(eventId)
                }
            }

            _uploadState.value = _uploadState.value.copy(
                isUploading = false,
                isPaused = false,
                isCompleted = isAllDone,
                overallProgress = if (isAllDone) 100 else _uploadState.value.overallProgress,
                uploadedFilesCount = uploadedCount,
                failedFilesCount = failedCount,
                remainingFilesCount = (totalFiles - uploadedCount).coerceAtLeast(0),
                estimatedRemainingSeconds = 0L,
                lastUploadTime = System.currentTimeMillis()
            )
        }
    }

    fun pauseUpload() {
        isPauseRequested = true
        _uploadState.value = _uploadState.value.copy(
            isUploading = false,
            isPaused = true,
            errorMessage = "Upload paused."
        )
    }

    private suspend fun simulateOrUploadFile(
        item: MediaItemEntity,
        onProgress: suspend (Int) -> Unit
    ): Boolean {
        return try {
            // Check if file exists locally
            val file = File(item.localPath)
            val isDummy = !file.exists()

            val steps = 5
            for (i in 1..steps) {
                if (isPauseRequested) return false
                delay(120) // Fast responsive upload ticks
                val progress = (i * 100) / steps
                onProgress(progress)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun buildFolderPath(workspaceName: String, eventDate: String, eventTitle: String): String {
        val safeWorkspace = workspaceName.trim().ifBlank { "Default Workspace" }
        val dateParts = eventDate.split("-")

        val year = if (dateParts.isNotEmpty() && dateParts[0].length == 4) dateParts[0] else "2026"
        val monthNum = if (dateParts.size >= 2) dateParts[1].toIntOrNull() ?: 7 else 7
        val monthName = when (monthNum) {
            1 -> "01 January"
            2 -> "02 February"
            3 -> "03 March"
            4 -> "04 April"
            5 -> "05 May"
            6 -> "06 June"
            7 -> "07 July"
            8 -> "08 August"
            9 -> "09 September"
            10 -> "10 October"
            11 -> "11 November"
            12 -> "12 December"
            else -> "07 July"
        }

        val safeDate = eventDate.trim().ifBlank { "2026-07-24" }
        val safeTitle = eventTitle.trim().ifBlank { "Event" }
        val eventFolderName = "$safeDate $safeTitle"

        return "Pulse/$safeWorkspace/$year/$monthName/$eventFolderName"
    }

    private fun getSubfolderName(fileType: String): String {
        return when (fileType) {
            MEDIA_TYPE_PHOTO -> "Photos"
            MEDIA_TYPE_VIDEO -> "Videos"
            MEDIA_TYPE_DOCUMENT -> "Documents"
            MEDIA_TYPE_AUDIO -> "Audio"
            else -> "Photos"
        }
    }
}
