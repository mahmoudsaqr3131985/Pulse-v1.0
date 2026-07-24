package com.example.services.ai

import com.example.models.AIContext
import com.example.models.GeneratedMediaPackage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class ClaudeProvider : AIProvider {
    override val providerName: String = "Claude"

    override suspend fun validateConnection(apiKey: String, model: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Claude API Key is empty"))
        }
        delay(300)
        if (apiKey.length < 5) {
            return@withContext Result.failure(IllegalArgumentException("Invalid Claude API Key format"))
        }
        Result.success(true)
    }

    override suspend fun generateMediaPackage(apiKey: String, model: String, context: AIContext): Result<GeneratedMediaPackage> = withContext(Dispatchers.IO) {
        val apiModel = AIHttpClient.mapModelToApiName("Claude", model)
        AIHttpClient.callClaude(apiKey, apiModel, context)
    }
}
