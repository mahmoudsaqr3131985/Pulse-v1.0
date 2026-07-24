package com.example.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val workspaceId: String,
    val title: String,
    val type: String,
    val date: String, // e.g. "2026-07-24" or formatted string
    val dateTimestamp: Long = System.currentTimeMillis(),
    val time: String? = null,
    val location: String? = null,
    val description: String? = null,
    val leader1Present: Boolean = false,
    val leader2Present: Boolean = false,
    val guestsJson: String = "[]", // Serialized list of GuestItem
    val status: String = EVENT_STATUS_PLANNED,

    // Section 5: Internal Statistics
    val photoCount: Int = 0,
    val videoCount: Int = 0,
    val documentCount: Int = 0,
    val audioCount: Int = 0,
    val generatedPost: String? = null,
    val generatedHashtags: String? = null,
    val uploadedToDrive: Boolean = false,
    val lastModified: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),

    // Phase 7 & 8: Publishing & Upload Preparation
    val publishingReady: Boolean = false,
    val publishingScore: Int = 0,
    val lastValidationTime: Long = 0L,
    val publishStatus: String = PUBLISH_STATUS_DRAFT,

    // Phase 8: Google Drive Upload Engine
    val driveFolderId: String? = null,
    val uploadStarted: Boolean = false,
    val uploadCompleted: Boolean = false,
    val uploadProgress: Int = 0,
    val uploadedFileCount: Int = 0,
    val failedFileCount: Int = 0,

    // Phase 9: Event Package & Metadata Engine
    val eventJsonPath: String? = null,
    val mediaJsonPath: String? = null,
    val workspaceJsonPath: String? = null,
    val manifestJsonPath: String? = null,
    val eventMarkdownPath: String? = null,
    val packageGenerated: Boolean = false,
    val packageGeneratedTime: Long = 0L,
    val packageError: String? = null
)

data class GuestItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val position: String,
    val organization: String
)

const val PUBLISH_STATUS_DRAFT = "Draft"
const val PUBLISH_STATUS_READY = "Ready"
const val PUBLISH_STATUS_PUBLISHING = "Publishing"
const val PUBLISH_STATUS_PUBLISHED = "Published"
const val PUBLISH_STATUS_FAILED = "Failed"

const val EVENT_STATUS_PLANNED = "Planned"
const val EVENT_STATUS_IN_PROGRESS = "In Progress"
const val EVENT_STATUS_COMPLETED = "Completed"
const val EVENT_STATUS_CANCELLED = "Cancelled"

val EVENT_TYPES = listOf(
    "Conference",
    "Seminar",
    "Workshop",
    "Student Activity",
    "Veterinary Convoy",
    "Scientific Visit",
    "Meeting",
    "Celebration",
    "Exam Follow-up",
    "Training",
    "Community Service",
    "Other"
)

val EVENT_STATUSES = listOf(
    EVENT_STATUS_PLANNED,
    EVENT_STATUS_IN_PROGRESS,
    EVENT_STATUS_COMPLETED,
    EVENT_STATUS_CANCELLED
)
