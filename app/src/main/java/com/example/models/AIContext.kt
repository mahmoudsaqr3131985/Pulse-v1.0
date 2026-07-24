package com.example.models

import org.json.JSONArray
import org.json.JSONObject

data class AIContext(
    val workspaceInfo: WorkspaceInfo,
    val leadership: LeadershipInfo,
    val eventInfo: EventInfo,
    val mediaStats: MediaStats,
    val guestList: List<String>,
    val publishingInfo: PublishingInfo,
    val metadataPaths: Map<String, String>,
    val language: String,
    val selectedAIProvider: String,
    val selectedAIModel: String,
    val temperature: Float,
    val maxTokens: Int,
    val writingProfile: String = "Official & Professional",
    val customInstructions: String = "",
    val generationTime: Long = System.currentTimeMillis(),
    val contextVersion: String = "1.0"
) {
    data class WorkspaceInfo(
        val id: String,
        val name: String,
        val type: String,
        val storageType: String,
        val driveFolderName: String?
    )

    data class LeadershipInfo(
        val leader1Title: String,
        val leader1Name: String,
        val leader1Present: Boolean,
        val leader2Title: String,
        val leader2Name: String,
        val leader2Present: Boolean
    )

    data class EventInfo(
        val id: String,
        val title: String,
        val date: String,
        val time: String,
        val location: String,
        val type: String,
        val status: String,
        val description: String,
        val hashtags: String
    )

    data class MediaStats(
        val totalItems: Int,
        val photoCount: Int,
        val videoCount: Int,
        val documentCount: Int,
        val audioCount: Int,
        val totalSizeBytes: Long
    )

    data class PublishingInfo(
        val publishStatus: String,
        val driveFolderId: String?,
        val localFolderPath: String?,
        val isPackageExported: Boolean
    )

    fun toFormattedJson(): String {
        val root = JSONObject()
        root.put("contextVersion", contextVersion)
        root.put("generationTime", generationTime)
        root.put("language", language)

        val aiConfig = JSONObject().apply {
            put("provider", selectedAIProvider)
            put("model", selectedAIModel)
            put("temperature", temperature)
            put("maxTokens", maxTokens)
            put("writingProfile", writingProfile)
            put("customInstructions", customInstructions)
        }
        root.put("aiSettings", aiConfig)

        val wsJson = JSONObject().apply {
            put("id", workspaceInfo.id)
            put("name", workspaceInfo.name)
            put("type", workspaceInfo.type)
            put("storageType", workspaceInfo.storageType)
            put("driveFolderName", workspaceInfo.driveFolderName ?: "")
        }
        root.put("workspace", wsJson)

        val leadJson = JSONObject().apply {
            put("leader1Title", leadership.leader1Title)
            put("leader1Name", leadership.leader1Name)
            put("leader1Present", leadership.leader1Present)
            put("leader2Title", leadership.leader2Title)
            put("leader2Name", leadership.leader2Name)
            put("leader2Present", leadership.leader2Present)
        }
        root.put("leadership", leadJson)

        val evJson = JSONObject().apply {
            put("id", eventInfo.id)
            put("title", eventInfo.title)
            put("date", eventInfo.date)
            put("time", eventInfo.time)
            put("location", eventInfo.location)
            put("type", eventInfo.type)
            put("status", eventInfo.status)
            put("description", eventInfo.description)
            put("hashtags", eventInfo.hashtags)
        }
        root.put("event", evJson)

        val mediaJson = JSONObject().apply {
            put("totalItems", mediaStats.totalItems)
            put("photoCount", mediaStats.photoCount)
            put("videoCount", mediaStats.videoCount)
            put("documentCount", mediaStats.documentCount)
            put("audioCount", mediaStats.audioCount)
            put("totalSizeBytes", mediaStats.totalSizeBytes)
        }
        root.put("mediaStats", mediaJson)

        val guestsArr = JSONArray()
        guestList.forEach { guestsArr.put(it) }
        root.put("guestList", guestsArr)

        val pubJson = JSONObject().apply {
            put("publishStatus", publishingInfo.publishStatus)
            put("driveFolderId", publishingInfo.driveFolderId ?: "")
            put("localFolderPath", publishingInfo.localFolderPath ?: "")
            put("isPackageExported", publishingInfo.isPackageExported)
        }
        root.put("publishingInfo", pubJson)

        val pathsJson = JSONObject()
        metadataPaths.forEach { (key, value) -> pathsJson.put(key, value) }
        root.put("metadataPaths", pathsJson)

        return root.toString(4)
    }
}
