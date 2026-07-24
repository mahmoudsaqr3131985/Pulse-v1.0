package com.example.models

/**
 * Model prepared for Phase 2+ Settings persistence.
 * Not active in Phase 1.
 */
data class UserSettingsModel(
    val appName: String = "Pulse",
    val version: String = "1.0",
    val codename: String = "Pulse",
    val institutionName: String = "Institutional Media Center",
    val isGoogleDriveConnected: Boolean = false,
    val isGoogleDocsConnected: Boolean = false,
    val isGeminiApiConfigured: Boolean = false
)
