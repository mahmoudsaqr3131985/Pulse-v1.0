package com.example.models

/**
 * Model prepared for Phase 2+ Gemini AI Assistant analysis tasks.
 * Not active in Phase 1.
 */
data class MediaAssistantModel(
    val taskId: String,
    val promptQuery: String,
    val generatedSummary: String? = null,
    val mediaTags: List<String> = emptyList(),
    val isProcessing: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
