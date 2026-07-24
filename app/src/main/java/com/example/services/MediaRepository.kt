package com.example.services

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.models.MEDIA_STATUS_READY
import com.example.models.MEDIA_TYPE_AUDIO
import com.example.models.MEDIA_TYPE_DOCUMENT
import com.example.models.MEDIA_TYPE_PHOTO
import com.example.models.MEDIA_TYPE_VIDEO
import com.example.models.MediaItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class MediaRepository(
    private val context: Context,
    private val mediaDao: MediaDao,
    private val eventDao: EventDao
) {

    fun getMediaForEvent(eventId: String): Flow<List<MediaItemEntity>> {
        return mediaDao.getMediaForEvent(eventId)
    }

    suspend fun getMediaById(id: String): MediaItemEntity? {
        return mediaDao.getMediaById(id)
    }

    suspend fun updateMedia(mediaItem: MediaItemEntity) {
        mediaDao.updateMedia(mediaItem)
    }

    suspend fun deleteMedia(mediaItem: MediaItemEntity) {
        withContext(Dispatchers.IO) {
            // Delete local file if it exists
            try {
                val file = File(mediaItem.localPath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mediaDao.deleteMedia(mediaItem)
            recalculateAndSyncEventStats(mediaItem.eventId)
        }
    }

    /**
     * Saves a temporary file (e.g. captured photo or video from Camera) into local workspace folder.
     */
    suspend fun addCapturedMedia(
        eventId: String,
        workspaceId: String,
        workspaceName: String,
        eventTitle: String,
        tempFile: File,
        fileType: String,
        mimeType: String
    ): MediaItemEntity = withContext(Dispatchers.IO) {
        val targetDir = getEventMediaDirectory(workspaceName, eventTitle)
        val finalFileName = "${fileType.lowercase()}_${System.currentTimeMillis()}.${tempFile.extension}"
        val destFile = File(targetDir, finalFileName)

        tempFile.copyTo(destFile, overwrite = true)
        tempFile.delete()

        val mediaItem = MediaItemEntity(
            eventId = eventId,
            workspaceId = workspaceId,
            fileName = destFile.name,
            fileType = fileType,
            mimeType = mimeType,
            localPath = destFile.absolutePath,
            fileSize = destFile.length(),
            createdAt = System.currentTimeMillis(),
            status = MEDIA_STATUS_READY
        )

        mediaDao.insertMedia(mediaItem)
        recalculateAndSyncEventStats(eventId)
        mediaItem
    }

    /**
     * Copies external URIs (from Gallery or File Picker) into the local workspace folder.
     */
    suspend fun addMediaFromUris(
        eventId: String,
        workspaceId: String,
        workspaceName: String,
        eventTitle: String,
        uris: List<Uri>
    ): List<MediaItemEntity> = withContext(Dispatchers.IO) {
        val targetDir = getEventMediaDirectory(workspaceName, eventTitle)
        val addedItems = mutableListOf<MediaItemEntity>()

        for (uri in uris) {
            try {
                val (fileName, fileSize, mimeType) = getUriMetadata(uri)
                val fileType = determineFileType(mimeType, fileName)
                val safeFileName = sanitizeFileName("${System.currentTimeMillis()}_$fileName")
                val destFile = File(targetDir, safeFileName)

                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(destFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                if (destFile.exists() && destFile.length() > 0) {
                    val mediaItem = MediaItemEntity(
                        eventId = eventId,
                        workspaceId = workspaceId,
                        fileName = fileName,
                        fileType = fileType,
                        mimeType = mimeType,
                        localPath = destFile.absolutePath,
                        fileSize = destFile.length(),
                        createdAt = System.currentTimeMillis(),
                        status = MEDIA_STATUS_READY
                    )
                    addedItems.add(mediaItem)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (addedItems.isNotEmpty()) {
            mediaDao.insertMediaItems(addedItems)
            recalculateAndSyncEventStats(eventId)
        }

        addedItems
    }

    suspend fun recalculateAndSyncEventStats(eventId: String) = withContext(Dispatchers.IO) {
        val event = eventDao.getEventById(eventId) ?: return@withContext
        val items = mediaDao.getMediaForEventSync(eventId)

        val photos = items.count { it.fileType == MEDIA_TYPE_PHOTO }
        val videos = items.count { it.fileType == MEDIA_TYPE_VIDEO }
        val docs = items.count { it.fileType == MEDIA_TYPE_DOCUMENT }
        val audios = items.count { it.fileType == MEDIA_TYPE_AUDIO }

        val updatedEvent = event.copy(
            photoCount = photos,
            videoCount = videos,
            documentCount = docs,
            audioCount = audios,
            lastModified = System.currentTimeMillis()
        )

        eventDao.insertOrUpdateEvent(updatedEvent)
    }

    private fun getEventMediaDirectory(workspaceName: String, eventTitle: String): File {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val safeWorkspace = sanitizeFolderName(workspaceName.ifBlank { "Default Workspace" })
        val safeEvent = sanitizeFolderName(eventTitle.ifBlank { "Event" })

        val eventDir = File(baseDir, "Pulse/$safeWorkspace/$safeEvent")
        if (!eventDir.exists()) {
            eventDir.mkdirs()
        }
        return eventDir
    }

    private fun sanitizeFolderName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9.-]"), "_")
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }

    private fun getUriMetadata(uri: Uri): Triple<String, Long, String> {
        var name = "file_${System.currentTimeMillis()}"
        var size = 0L
        val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
                if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
            }
        }
        return Triple(name, size, mimeType)
    }

    private fun determineFileType(mimeType: String, fileName: String): String {
        val lowerMime = mimeType.lowercase()
        val lowerExt = fileName.substringAfterLast('.', "").lowercase()

        return when {
            lowerMime.startsWith("image/") || lowerExt in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic") -> MEDIA_TYPE_PHOTO
            lowerMime.startsWith("video/") || lowerExt in listOf("mp4", "mkv", "mov", "avi", "3gp", "webm") -> MEDIA_TYPE_VIDEO
            lowerMime.startsWith("audio/") || lowerExt in listOf("mp3", "wav", "m4a", "aac", "ogg", "flac") -> MEDIA_TYPE_AUDIO
            else -> MEDIA_TYPE_DOCUMENT
        }
    }
}
