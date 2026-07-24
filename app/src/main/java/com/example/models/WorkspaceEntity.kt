package com.example.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "workspaces")
data class WorkspaceEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: String,
    val leader1Title: String,
    val leader1Name: String,
    val leader2Title: String? = null,
    val leader2Name: String? = null,
    val defaultHashtags: String = "",
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    
    // Phase 3 & Phase 4: Storage Configuration Fields
    val storageType: String = STORAGE_TYPE_UNCONFIGURED,
    val storageStatus: String = STORAGE_STATUS_NOT_CONFIGURED,
    val driveFolderChoice: String? = null,
    val driveFolderId: String? = null,
    val driveFolderName: String? = null,
    val localFolderName: String? = null,
    val driveConnectionPending: Boolean = false,
    
    // Phase 4: Google Drive Authentication & Account Storage
    val googleAccountEmail: String? = null,
    val lastConnectionTime: Long? = null,

    // Phase 10: Artificial Intelligence Settings
    val aiProvider: String = AI_PROVIDER_NONE,
    val aiApiKey: String? = null,
    val aiModel: String = AI_MODEL_NONE,
    val aiTemperature: Float = 0.4f,
    val aiMaxTokens: Int = 2048,
    val aiLanguage: String = AI_LANGUAGE_AUTO,
    val aiConnectionStatus: String = AI_STATUS_NOT_CONFIGURED,
    val aiLastValidationTime: Long = 0L
)

// Storage Constants
const val STORAGE_TYPE_UNCONFIGURED = "UNCONFIGURED"
const val STORAGE_TYPE_GOOGLE_DRIVE = "GOOGLE_DRIVE"
const val STORAGE_TYPE_LOCAL_STORAGE = "LOCAL_STORAGE"

const val STORAGE_STATUS_NOT_CONFIGURED = "NOT_CONFIGURED"
const val STORAGE_STATUS_READY = "READY"
const val STORAGE_STATUS_CONNECTED = "CONNECTED"
const val STORAGE_STATUS_PENDING = "PENDING"

const val DRIVE_CHOICE_CREATE_NEW = "CREATE_NEW"
const val DRIVE_CHOICE_CHOOSE_EXISTING = "CHOOSE_EXISTING"
const val DRIVE_CHOICE_SKIP = "SKIP"

// Phase 10: AI Constants
const val AI_PROVIDER_NONE = "None"
const val AI_PROVIDER_GEMINI = "Gemini"
const val AI_PROVIDER_OPENAI = "OpenAI"
const val AI_PROVIDER_CLAUDE = "Claude"
const val AI_PROVIDER_OPENROUTER = "OpenRouter"
const val AI_PROVIDER_CUSTOM = "Custom API"

const val AI_STATUS_NOT_CONFIGURED = "Not Configured"
const val AI_STATUS_CONNECTED = "Connected"
const val AI_STATUS_FAILED = "Failed"

const val AI_LANGUAGE_ARABIC = "Arabic"
const val AI_LANGUAGE_ENGLISH = "English"
const val AI_LANGUAGE_AUTO = "Auto"

const val AI_MODEL_NONE = "None"

val AI_PROVIDERS = listOf(
    AI_PROVIDER_NONE,
    AI_PROVIDER_GEMINI,
    AI_PROVIDER_OPENAI,
    AI_PROVIDER_CLAUDE,
    AI_PROVIDER_OPENROUTER,
    AI_PROVIDER_CUSTOM
)

val AI_LANGUAGES = listOf(
    AI_LANGUAGE_AUTO,
    AI_LANGUAGE_ENGLISH,
    AI_LANGUAGE_ARABIC
)

fun getModelsForProvider(provider: String): List<String> {
    return when (provider) {
        AI_PROVIDER_GEMINI -> listOf("Gemini Flash", "Gemini Pro")
        AI_PROVIDER_OPENAI -> listOf("GPT-4o", "GPT-4o-mini", "GPT-3.5-Turbo")
        AI_PROVIDER_CLAUDE -> listOf("Claude 3.5 Sonnet", "Claude 3 Haiku")
        AI_PROVIDER_OPENROUTER -> listOf("Default (Auto)")
        AI_PROVIDER_CUSTOM -> listOf("Custom Model")
        else -> listOf("None")
    }
}

val WORKSPACE_TYPES = listOf(
    "Faculty",
    "University",
    "School",
    "Hospital",
    "Company",
    "Media Center",
    "Association",
    "Government Organization",
    "Other"
)
