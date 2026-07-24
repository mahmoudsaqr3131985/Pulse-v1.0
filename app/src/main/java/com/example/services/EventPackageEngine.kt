package com.example.services

import android.content.Context
import com.example.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventPackageEngine private constructor(
    private val context: Context,
    private val eventDao: EventDao,
    private val workspaceDao: WorkspaceDao,
    private val mediaDao: MediaDao
) {

    companion object {
        @Volatile
        private var instance: EventPackageEngine? = null

        fun getInstance(
            context: Context,
            eventDao: EventDao,
            workspaceDao: WorkspaceDao,
            mediaDao: MediaDao
        ): EventPackageEngine {
            return instance ?: synchronized(this) {
                instance ?: EventPackageEngine(
                    context.applicationContext,
                    eventDao,
                    workspaceDao,
                    mediaDao
                ).also { instance = it }
            }
        }
    }

    suspend fun generatePackage(eventId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val event = eventDao.getEventById(eventId)
                ?: throw IllegalArgumentException("Event not found: $eventId")
            val workspace = workspaceDao.getWorkspaceById(event.workspaceId)
            val mediaList = mediaDao.getMediaForEventSync(eventId)

            // Package Directory
            val packageDir = File(context.filesDir, "packages/$eventId")
            if (!packageDir.exists()) {
                packageDir.mkdirs()
            }

            // Folder structure path
            val folderPath = buildFolderPath(
                workspaceName = workspace?.name ?: "Default Workspace",
                eventDate = event.date,
                eventTitle = event.title
            )

            // 1. Generate event.json
            val eventJsonObj = JSONObject().apply {
                put("eventId", event.id)
                put("workspaceId", event.workspaceId)
                put("title", event.title)
                put("type", event.type)
                put("date", event.date)
                put("time", event.time ?: "")
                put("location", event.location ?: "")
                put("description", event.description ?: "")
                put("status", event.status)
                put("publishingStatus", event.publishStatus)

                put("leaderAttendance", JSONObject().apply {
                    put("leader1Present", event.leader1Present)
                    put("leader1Title", workspace?.leader1Title ?: "Leader 1")
                    put("leader1Name", workspace?.leader1Name ?: "")
                    put("leader2Present", event.leader2Present)
                    put("leader2Title", workspace?.leader2Title ?: "Leader 2")
                    put("leader2Name", workspace?.leader2Name ?: "")
                })

                // Parse guests list
                val guestsArr = JSONArray()
                runCatching {
                    val rawGuests = JSONArray(event.guestsJson)
                    for (i in 0 until rawGuests.length()) {
                        guestsArr.put(rawGuests.get(i))
                    }
                }
                put("guestList", guestsArr)

                put("photoCount", event.photoCount)
                put("videoCount", event.videoCount)
                put("documentCount", event.documentCount)
                put("audioCount", event.audioCount)
                put("creationTime", event.createdAt)
                put("lastModifiedTime", event.lastModified)
            }

            val eventJsonFile = File(packageDir, "event.json")
            eventJsonFile.writeText(eventJsonObj.toString(4))

            // 2. Generate media.json
            val mediaJsonArr = JSONArray()
            for (media in mediaList) {
                val subfolder = when (media.fileType) {
                    MEDIA_TYPE_PHOTO -> "Photos"
                    MEDIA_TYPE_VIDEO -> "Videos"
                    MEDIA_TYPE_DOCUMENT -> "Documents"
                    MEDIA_TYPE_AUDIO -> "Audio"
                    else -> "Photos"
                }
                val ext = if (media.fileName.contains(".")) media.fileName.substringAfterLast(".") else media.fileType.lowercase()

                val itemObj = JSONObject().apply {
                    put("mediaId", media.id)
                    put("fileName", media.fileName)
                    put("driveFileId", media.driveFileId ?: "")
                    put("driveFolder", "$folderPath/$subfolder")
                    put("mediaType", media.fileType)
                    put("extension", ext)
                    put("size", media.fileSize)
                    put("captureDate", media.createdAt)
                    put("uploadDate", if (media.uploadTime > 0) media.uploadTime else media.createdAt)
                    put("description", media.description ?: "")
                }
                mediaJsonArr.put(itemObj)
            }

            val mediaJsonFile = File(packageDir, "media.json")
            mediaJsonFile.writeText(mediaJsonArr.toString(4))

            // 3. Generate workspace.json
            val workspaceJsonObj = JSONObject().apply {
                put("workspaceName", workspace?.name ?: "Default Workspace")
                put("workspaceType", workspace?.type ?: "Organization")
                put("leader1Title", workspace?.leader1Title ?: "Leader 1")
                put("leader1Name", workspace?.leader1Name ?: "")
                put("leader2Title", workspace?.leader2Title ?: "")
                put("leader2Name", workspace?.leader2Name ?: "")
                put("defaultHashtags", workspace?.defaultHashtags ?: "")
                put("writingProfile", "Default formal writing profile (Placeholder)")
                put("customInstructions", "Standard organizational publishing guidelines (Placeholder)")
                put("storageType", workspace?.storageType ?: STORAGE_TYPE_UNCONFIGURED)
                put("googleAccountEmail", workspace?.googleAccountEmail ?: "")
            }

            val workspaceJsonFile = File(packageDir, "workspace.json")
            workspaceJsonFile.writeText(workspaceJsonObj.toString(4))

            // 4. Generate manifest.json
            val manifestJsonObj = JSONObject().apply {
                put("schemaVersion", "1.0")
                put("packageVersion", "1.0")
                put("workspaceId", event.workspaceId)
                put("eventId", event.id)
                put("generatedTime", System.currentTimeMillis())
                put("totalFiles", mediaList.size)
                put("folderStructure", JSONArray().apply {
                    put("$folderPath/Photos")
                    put("$folderPath/Videos")
                    put("$folderPath/Documents")
                    put("$folderPath/Audio")
                })
                put("checksum", "SHA-256-PLACEHOLDER")
                put("futureCompatibilityVersion", "1.0")
            }

            val manifestJsonFile = File(packageDir, "manifest.json")
            manifestJsonFile.writeText(manifestJsonObj.toString(4))

            // 5. Generate event.md
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val pubDateStr = sdf.format(Date())

            val mdBuilder = StringBuilder().apply {
                appendLine("# ${event.title}")
                appendLine()
                appendLine("## Event Information")
                appendLine("- **Event ID:** ${event.id}")
                appendLine("- **Type:** ${event.type}")
                appendLine("- **Date:** ${event.date}")
                appendLine("- **Time:** ${event.time ?: "Not specified"}")
                appendLine("- **Location:** ${event.location ?: "Not specified"}")
                appendLine("- **Status:** ${event.status}")
                appendLine("- **Publishing Status:** ${event.publishStatus}")
                appendLine("- **Description:** ${event.description ?: "None"}")
                appendLine()
                appendLine("## Leadership")
                val l1Title = workspace?.leader1Title ?: "Leader 1"
                val l1Name = workspace?.leader1Name ?: "Unspecified"
                val l1Att = if (event.leader1Present) "Present" else "Absent"
                appendLine("- **$l1Title:** $l1Name ($l1Att)")

                if (!workspace?.leader2Title.isNullOrBlank()) {
                    val l2Title = workspace!!.leader2Title!!
                    val l2Name = workspace.leader2Name ?: "Unspecified"
                    val l2Att = if (event.leader2Present) "Present" else "Absent"
                    appendLine("- **$l2Title:** $l2Name ($l2Att)")
                }
                appendLine()
                appendLine("## Guests")
                val deserializeGuests = deserializeGuestsList(event.guestsJson)
                if (deserializeGuests.isEmpty()) {
                    appendLine("- No guests recorded.")
                } else {
                    for (g in deserializeGuests) {
                        appendLine("- **${g.name}** (${g.position} - ${g.organization})")
                    }
                }
                appendLine()
                appendLine("## Media Statistics")
                appendLine("- **Photos:** ${event.photoCount}")
                appendLine("- **Videos:** ${event.videoCount}")
                appendLine("- **Documents:** ${event.documentCount}")
                appendLine("- **Audio:** ${event.audioCount}")
                appendLine("- **Total Files:** ${mediaList.size}")
                appendLine()
                appendLine("## Google Drive Folder")
                appendLine("`$folderPath`")
                appendLine()
                appendLine("## Publishing Date")
                appendLine(pubDateStr)
            }

            val mdFile = File(packageDir, "event.md")
            mdFile.writeText(mdBuilder.toString())

            // Update Database with Package Information
            val updatedEvent = event.copy(
                eventJsonPath = eventJsonFile.absolutePath,
                mediaJsonPath = mediaJsonFile.absolutePath,
                workspaceJsonPath = workspaceJsonFile.absolutePath,
                manifestJsonPath = manifestJsonFile.absolutePath,
                eventMarkdownPath = mdFile.absolutePath,
                packageGenerated = true,
                packageGeneratedTime = System.currentTimeMillis(),
                packageError = null,
                publishStatus = PUBLISH_STATUS_PUBLISHED,
                lastModified = System.currentTimeMillis()
            )
            eventDao.insertOrUpdateEvent(updatedEvent)

            true
        }
    }

    private fun deserializeGuestsList(jsonStr: String): List<GuestItem> {
        return runCatching {
            val list = mutableListOf<GuestItem>()
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    GuestItem(
                        id = obj.optString("id"),
                        name = obj.optString("name"),
                        position = obj.optString("position"),
                        organization = obj.optString("organization")
                    )
                )
            }
            list
        }.getOrDefault(emptyList())
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
}
