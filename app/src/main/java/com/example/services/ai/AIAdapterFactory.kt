package com.example.services.ai

import com.example.models.*

object AIAdapterFactory {
    fun getProvider(providerName: String): AIProvider {
        return when (providerName) {
            AI_PROVIDER_GEMINI -> GeminiProvider()
            AI_PROVIDER_OPENAI -> OpenAIProvider()
            AI_PROVIDER_CLAUDE -> ClaudeProvider()
            AI_PROVIDER_OPENROUTER -> OpenRouterProvider()
            AI_PROVIDER_CUSTOM -> CustomProvider()
            else -> NoneAIProvider()
        }
    }
}
