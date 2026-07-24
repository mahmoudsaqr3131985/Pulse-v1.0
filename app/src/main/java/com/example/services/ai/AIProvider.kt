package com.example.services.ai

import com.example.models.AIContext
import com.example.models.GeneratedMediaPackage

interface AIProvider {
    val providerName: String
    suspend fun validateConnection(apiKey: String, model: String): Result<Boolean>
    suspend fun generateMediaPackage(apiKey: String, model: String, context: AIContext): Result<GeneratedMediaPackage>
}
