package com.example.services.ai

import com.example.models.AIContext
import com.example.models.GeneratedMediaPackage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class GeminiProvider : AIProvider {
    override val providerName: String = "Gemini"

    override suspend fun validateConnection(apiKey: String, model: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Gemini API Key is empty"))
        }
        // Lightweight simulated check
        delay(300)
        if (apiKey.length < 5) {
            return@withContext Result.failure(IllegalArgumentException("Invalid Gemini API Key format"))
        }
        Result.success(true)
    }

    override suspend fun generateMediaPackage(apiKey: String, model: String, context: AIContext): Result<GeneratedMediaPackage> = withContext(Dispatchers.IO) {
        val apiModel = AIHttpClient.mapModelToApiName("Gemini", model)
        AIHttpClient.callGemini(apiKey, apiModel, context)
    }
}
