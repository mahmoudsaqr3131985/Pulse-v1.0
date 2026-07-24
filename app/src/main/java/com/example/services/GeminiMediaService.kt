package com.example.services

import com.example.models.MediaAssistantModel

/**
 * Service contract prepared for future Gemini API integration.
 * DO NOT implement in Phase 1.
 */
interface GeminiMediaService {
    suspend fun analyzeMediaPrompt(prompt: String): Result<MediaAssistantModel>
    suspend fun generatePressReleaseSummary(eventContext: String): Result<String>
}
