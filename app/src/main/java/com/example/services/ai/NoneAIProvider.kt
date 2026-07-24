package com.example.services.ai

import com.example.models.AIContext
import com.example.models.GeneratedMediaPackage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NoneAIProvider : AIProvider {
    override val providerName: String = "None"

    override suspend fun validateConnection(apiKey: String, model: String): Result<Boolean> = withContext(Dispatchers.IO) {
        Result.failure(IllegalStateException("No AI Provider configured for this Workspace"))
    }

    override suspend fun generateMediaPackage(apiKey: String, model: String, context: AIContext): Result<GeneratedMediaPackage> = withContext(Dispatchers.IO) {
        Result.failure(IllegalStateException("No AI Provider configured for this Workspace"))
    }
}
