package com.example.services.ai

import com.example.models.AIContext
import com.example.models.GeneratedMediaPackage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CustomProvider : AIProvider {
    override val providerName: String = "Custom API"

    override suspend fun validateConnection(apiKey: String, model: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Custom API Key is empty"))
        }
        Result.success(true)
    }

    override suspend fun generateMediaPackage(apiKey: String, model: String, context: AIContext): Result<GeneratedMediaPackage> = withContext(Dispatchers.IO) {
        Result.failure(IllegalStateException("Custom API Provider content generation is not supported in this version."))
    }
}
