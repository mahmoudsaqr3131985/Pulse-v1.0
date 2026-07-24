package com.example.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val eventId: String,
    val workspaceId: String,
    val fileName: String,
    val fileType: String, // MEDIA_TYPE_PHOTO, MEDIA_TYPE_VIDEO, MEDIA_TYPE_DOCUMENT, MEDIA_TYPE_AUDIO
    val mimeType: String,
    val localPath: String,
    val fileSize: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = MEDIA_STATUS_READY, // MEDIA_STATUS_PENDING, MEDIA_STATUS_READY, MEDIA_STATUS_UPLOADED, MEDIA_STATUS_FAILED
    val description: String? = null,

    // Phase 8: Upload properties
    val driveFileId: String? = null,
    val uploadStatus: String = UPLOAD_STATUS_PENDING,
    val uploadTime: Long = 0L
)

const val MEDIA_TYPE_PHOTO = "PHOTO"
const val MEDIA_TYPE_VIDEO = "VIDEO"
const val MEDIA_TYPE_DOCUMENT = "DOCUMENT"
const val MEDIA_TYPE_AUDIO = "AUDIO"

const val MEDIA_STATUS_PENDING = "Pending"
const val MEDIA_STATUS_READY = "Ready"
const val MEDIA_STATUS_UPLOADED = "Uploaded"
const val MEDIA_STATUS_FAILED = "Failed"

const val UPLOAD_STATUS_PENDING = "Pending"
const val UPLOAD_STATUS_UPLOADING = "Uploading"
const val UPLOAD_STATUS_UPLOADED = "Uploaded"
const val UPLOAD_STATUS_FAILED = "Failed"
const val UPLOAD_STATUS_SKIPPED = "Skipped"
