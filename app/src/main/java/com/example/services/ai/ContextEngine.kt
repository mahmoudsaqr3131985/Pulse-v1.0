package com.example.services.ai

import android.content.Context
import com.example.models.*
import com.example.utils.EncryptionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ContextEngine private constructor() {

    companion object {
        @Volatile
        private var instance: ContextEngine? = null

        fun getInstance(): ContextEngine {
            return instance ?: synchronized(this) {
                instance ?: ContextEngine().also { instance = it }
            }
        }
    }

    suspend fun buildContext(
        context: Context,
        workspace: WorkspaceEntity,
        event: EventEntity,
        mediaItems: List<MediaItemEntity>
    ): AIContext = withContext(Dispatchers.IO) {
        val packageDir = File(context.filesDir, "packages/${event.id}")
        val metadataPathsMap = mutableMapOf<String, String>()

        listOf("event.json", "media.json", "workspace.json", "manifest.json", "event.md").forEach { fileName ->
            val file = File(packageDir, fileName)
            if (file.exists()) {
                metadataPathsMap[fileName] = file.absolutePath
            } else {
                metadataPathsMap[fileName] = "Not Generated"
            }
        }

        // Parse guests list
        val guestList = mutableListOf<String>()
        runCatching {
            val jsonArr = JSONArray(event.guestsJson)
            for (i in 0 until jsonArr.length()) {
                guestList.add(jsonArr.getString(i))
            }
        }

        val totalSize = mediaItems.sumOf { it.fileSize }
        val dateStr = event.date

        AIContext(
            workspaceInfo = AIContext.WorkspaceInfo(
                id = workspace.id,
                name = workspace.name,
                type = workspace.type,
                storageType = workspace.storageType,
                driveFolderName = workspace.driveFolderName
            ),
            leadership = AIContext.LeadershipInfo(
                leader1Title = workspace.leader1Title ?: "Leader 1",
                leader1Name = workspace.leader1Name ?: "",
                leader1Present = event.leader1Present,
                leader2Title = workspace.leader2Title ?: "Leader 2",
                leader2Name = workspace.leader2Name ?: "",
                leader2Present = event.leader2Present
            ),
            eventInfo = AIContext.EventInfo(
                id = event.id,
                title = event.title,
                date = dateStr,
                time = event.time ?: "",
                location = event.location ?: "",
                type = event.type,
                status = event.status,
                description = event.description ?: "",
                hashtags = event.generatedHashtags ?: workspace.defaultHashtags
            ),
            mediaStats = AIContext.MediaStats(
                totalItems = mediaItems.size,
                photoCount = mediaItems.count { it.fileType == MEDIA_TYPE_PHOTO },
                videoCount = mediaItems.count { it.fileType == MEDIA_TYPE_VIDEO },
                documentCount = mediaItems.count { it.fileType == MEDIA_TYPE_DOCUMENT },
                audioCount = mediaItems.count { it.fileType == MEDIA_TYPE_AUDIO },
                totalSizeBytes = totalSize
            ),
            guestList = guestList,
            publishingInfo = AIContext.PublishingInfo(
                publishStatus = event.publishStatus,
                driveFolderId = event.driveFolderId,
                localFolderPath = packageDir.absolutePath,
                isPackageExported = packageDir.exists() && File(packageDir, "manifest.json").exists()
            ),
            metadataPaths = metadataPathsMap,
            language = workspace.aiLanguage,
            selectedAIProvider = workspace.aiProvider,
            selectedAIModel = workspace.aiModel,
            temperature = workspace.aiTemperature,
            maxTokens = workspace.aiMaxTokens,
            writingProfile = "Official & Professional",
            customInstructions = ""
        )
    }

    fun validateContext(
        workspace: WorkspaceEntity?,
        event: EventEntity?,
        packageDir: File?
    ): ContextValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (workspace == null) {
            errors.add("Workspace does not exist.")
            return ContextValidationResult(isValid = false, errors = errors, warnings = warnings)
        }

        if (event == null) {
            errors.add("Event does not exist.")
            return ContextValidationResult(isValid = false, errors = errors, warnings = warnings)
        }

        if (workspace.aiProvider == AI_PROVIDER_NONE || workspace.aiProvider.isBlank()) {
            errors.add("No AI Provider selected for Workspace.")
        }

        val decryptedKey = EncryptionUtils.decrypt(workspace.aiApiKey)
        if (decryptedKey.isNullOrBlank()) {
            errors.add("API Key is missing for AI Provider (${workspace.aiProvider}).")
        }

        if (packageDir == null || !packageDir.exists()) {
            warnings.add("Event Package directory not found. Metadata JSON files are missing.")
        } else {
            val manifest = File(packageDir, "manifest.json")
            if (!manifest.exists()) {
                warnings.add("Package manifest.json is missing. Please generate Event Package.")
            }
        }

        return ContextValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    suspend fun exportContext(
        context: Context,
        aiContext: AIContext,
        eventId: String
    ): File = withContext(Dispatchers.IO) {
        val exportDir = File(context.filesDir, "exported_contexts")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        val exportFile = File(exportDir, "aicontext_$eventId.json")
        exportFile.writeText(aiContext.toFormattedJson())
        exportFile
    }
}
