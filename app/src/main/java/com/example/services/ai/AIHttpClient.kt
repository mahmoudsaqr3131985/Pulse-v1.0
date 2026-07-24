package com.example.services.ai

import com.example.models.AIContext
import com.example.models.GeneratedMediaPackage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object AIHttpClient {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun mapModelToApiName(provider: String, displayModel: String): String {
        return when (provider) {
            "Gemini" -> {
                if (displayModel.contains("Pro", ignoreCase = true)) "gemini-3.1-pro-preview" else "gemini-3.5-flash"
            }
            "OpenAI" -> {
                when {
                    displayModel.contains("mini", ignoreCase = true) -> "gpt-4o-mini"
                    displayModel.contains("3.5", ignoreCase = true) -> "gpt-3.5-turbo"
                    else -> "gpt-4o"
                }
            }
            "Claude" -> {
                if (displayModel.contains("Haiku", ignoreCase = true)) "claude-3-haiku-20240307" else "claude-3-5-sonnet-latest"
            }
            "OpenRouter" -> {
                "google/gemini-2.5-flash"
            }
            else -> displayModel
        }
    }

    fun buildSystemInstruction(language: String): String {
        return "You are Pulse AI, an expert content generation system for official institutions. " +
                "You must construct a complete media package based on the provided event metadata. " +
                "Your response must be exactly a single, valid JSON object with the keys: " +
                "\"headline\", \"facebookPost\", \"shortPost\", \"caption\", \"hashtags\", \"newsSummary\", \"voiceOverScript\". " +
                "Do not include any conversational intro, outro, or additional markdown outside the JSON."
    }

    fun buildUserPrompt(context: AIContext): String {
        val guests = context.guestList.joinToString(", ")
        val langTarget = when (context.language.lowercase()) {
            "arabic" -> "Arabic"
            "english" -> "English"
            "arabic_english", "arabic + english", "العربية + english" -> "Arabic + English (generate bilingual content for EACH field, clearly separating Arabic and English text within that field's value)"
            else -> context.language
        }

        val leadersPresent = mutableListOf<String>()
        if (context.leadership.leader1Present && context.leadership.leader1Name.isNotBlank()) {
            leadersPresent.add("${context.leadership.leader1Title}: ${context.leadership.leader1Name}")
        }
        if (context.leadership.leader2Present && context.leadership.leader2Name.isNotBlank()) {
            leadersPresent.add("${context.leadership.leader2Title}: ${context.leadership.leader2Name}")
        }
        val leadersStr = if (leadersPresent.isEmpty()) "None" else leadersPresent.joinToString(", ")

        return """
            Generate a media package in $langTarget language.
            
            Event Details:
            - Title: ${context.eventInfo.title}
            - Type: ${context.eventInfo.type}
            - Date: ${context.eventInfo.date}
            - Time: ${context.eventInfo.time}
            - Location: ${context.eventInfo.location}
            - Status: ${context.eventInfo.status}
            - Description: ${context.eventInfo.description}
            - Default Hashtags: ${context.eventInfo.hashtags}
            
            Attendees & Leadership Present:
            - Workspace Leaders Present: $leadersStr
            - Distinguished Guests: $guests
            
            Media Stats:
            - Photos: ${context.mediaStats.photoCount}
            - Videos: ${context.mediaStats.videoCount}
            - Documents: ${context.mediaStats.documentCount}
            - Audios: ${context.mediaStats.audioCount}
            
            Generate professional, highly engaging, and contextual copy for these 7 fields:
            1. "headline" (An elegant, high-impact news headline)
            2. "facebookPost" (A structured, engaging social media post with emojis and spacing)
            3. "shortPost" (A crisp micro-post suitable for X/Twitter/Telegram)
            4. "caption" (A concise, descriptive picture caption)
            5. "hashtags" (Relevant, optimized hashtags including the default hashtags)
            6. "newsSummary" (A formal news summary/article for institutional websites)
            7. "voiceOverScript" (A promotional promo video voice-over script, including narrator cues and background scene hints)
            
            Output ONLY the valid JSON object containing these 7 keys.
        """.trimIndent()
    }

    fun parseResponse(rawText: String): GeneratedMediaPackage {
        var cleanJson = rawText.trim()
        if (cleanJson.contains("```json")) {
            cleanJson = cleanJson.substringAfter("```json").substringBefore("```")
        } else if (cleanJson.contains("```")) {
            cleanJson = cleanJson.substringAfter("```").substringBefore("```")
        }
        cleanJson = cleanJson.trim()

        try {
            val obj = JSONObject(cleanJson)
            return GeneratedMediaPackage(
                headline = obj.optString("headline", "").trim(),
                facebookPost = obj.optString("facebookPost", "").trim(),
                shortPost = obj.optString("shortPost", "").trim(),
                caption = obj.optString("caption", "").trim(),
                hashtags = obj.optString("hashtags", "").trim(),
                newsSummary = obj.optString("newsSummary", "").trim(),
                voiceOverScript = obj.optString("voiceOverScript", "").trim()
            )
        } catch (e: Exception) {
            // Fallback parsing if JSON is partially corrupted or direct string
            val headlineMatch = Regex("\"headline\"\\s*:\\s*\"([^\"]*)\"").find(cleanJson)
            val fbMatch = Regex("\"facebookPost\"\\s*:\\s*\"([^\"]*)\"").find(cleanJson)
            val shortMatch = Regex("\"shortPost\"\\s*:\\s*\"([^\"]*)\"").find(cleanJson)
            val captionMatch = Regex("\"caption\"\\s*:\\s*\"([^\"]*)\"").find(cleanJson)
            val hashtagsMatch = Regex("\"hashtags\"\\s*:\\s*\"([^\"]*)\"").find(cleanJson)
            val newsMatch = Regex("\"newsSummary\"\\s*:\\s*\"([^\"]*)\"").find(cleanJson)
            val voMatch = Regex("\"voiceOverScript\"\\s*:\\s*\"([^\"]*)\"").find(cleanJson)

            if (headlineMatch != null || fbMatch != null || newsMatch != null) {
                return GeneratedMediaPackage(
                    headline = headlineMatch?.groupValues?.getOrNull(1) ?: "Generated Event Package",
                    facebookPost = fbMatch?.groupValues?.getOrNull(1) ?: rawText,
                    shortPost = shortMatch?.groupValues?.getOrNull(1) ?: "",
                    caption = captionMatch?.groupValues?.getOrNull(1) ?: "",
                    hashtags = hashtagsMatch?.groupValues?.getOrNull(1) ?: "",
                    newsSummary = newsMatch?.groupValues?.getOrNull(1) ?: "",
                    voiceOverScript = voMatch?.groupValues?.getOrNull(1) ?: ""
                )
            }
            // Absolute fallback
            return GeneratedMediaPackage(
                headline = "Generated Package",
                facebookPost = rawText,
                shortPost = rawText.take(280),
                caption = rawText.take(150),
                hashtags = "#pulse",
                newsSummary = rawText,
                voiceOverScript = rawText
            )
        }
    }

    suspend fun callGemini(apiKey: String, modelName: String, context: AIContext): Result<GeneratedMediaPackage> {
        val model = modelName.ifBlank { "gemini-3.5-flash" }
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val systemText = buildSystemInstruction(context.language)
        val userPrompt = buildUserPrompt(context)

        val requestBodyJson = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", userPrompt)
                        })
                    })
                })
            }
            put("contents", contentsArray)

            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", systemText)
                    })
                })
            })

            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", context.temperature)
                put("maxOutputTokens", context.maxTokens)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
            .build()

        return executeRequest(request)
    }

    suspend fun callOpenAI(apiKey: String, modelName: String, context: AIContext, baseUrl: String = "https://api.openai.com/v1/chat/completions"): Result<GeneratedMediaPackage> {
        val model = modelName.ifBlank { "gpt-4o-mini" }

        val systemText = buildSystemInstruction(context.language)
        val userPrompt = buildUserPrompt(context)

        val requestBodyJson = JSONObject().apply {
            put("model", model)
            val messagesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemText)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            }
            put("messages", messagesArray)
            put("temperature", context.temperature)
            put("max_tokens", context.maxTokens)
            put("response_format", JSONObject().apply {
                put("type", "json_object")
            })
        }

        val request = Request.Builder()
            .url(baseUrl)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
            .build()

        return executeRequestOpenAI(request)
    }

    suspend fun callClaude(apiKey: String, modelName: String, context: AIContext): Result<GeneratedMediaPackage> {
        val model = modelName.ifBlank { "claude-3-5-sonnet-latest" }
        val url = "https://api.anthropic.com/v1/messages"

        val systemText = buildSystemInstruction(context.language)
        val userPrompt = buildUserPrompt(context)

        val requestBodyJson = JSONObject().apply {
            put("model", model)
            put("system", systemText)
            put("max_tokens", context.maxTokens)
            put("temperature", context.temperature)
            val messagesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            }
            put("messages", messagesArray)
        }

        val request = Request.Builder()
            .url(url)
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("Content-Type", "application/json")
            .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
            .build()

        return executeRequestClaude(request)
    }

    private fun executeRequest(request: Request): Result<GeneratedMediaPackage> {
        return try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return Result.failure(IOException("HTTP Error ${response.code}: $bodyString"))
                }
                
                val responseJson = JSONObject(bodyString)
                val text = responseJson.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                Result.success(parseResponse(text))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun executeRequestOpenAI(request: Request): Result<GeneratedMediaPackage> {
        return try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return Result.failure(IOException("HTTP Error ${response.code}: $bodyString"))
                }

                val responseJson = JSONObject(bodyString)
                val text = responseJson.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")

                Result.success(parseResponse(text))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun executeRequestClaude(request: Request): Result<GeneratedMediaPackage> {
        return try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return Result.failure(IOException("HTTP Error ${response.code}: $bodyString"))
                }

                val responseJson = JSONObject(bodyString)
                val text = responseJson.getJSONArray("content")
                    .getJSONObject(0)
                    .getString("text")

                Result.success(parseResponse(text))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateText(
        provider: String,
        apiKey: String,
        modelName: String,
        systemInstruction: String,
        prompt: String
    ): Result<String> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val model = mapModelToApiName(provider, modelName)
        try {
            when (provider) {
                "Gemini" -> {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                    val requestBodyJson = JSONObject().apply {
                        put("contents", JSONArray().apply {
                            put(JSONObject().apply {
                                put("parts", JSONArray().apply {
                                    put(JSONObject().apply { put("text", prompt) })
                                })
                            })
                        })
                        put("systemInstruction", JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply { put("text", systemInstruction) })
                            })
                        })
                    }
                    val request = Request.Builder()
                        .url(url)
                        .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                        .build()
                    client.newCall(request).execute().use { response ->
                        val bodyString = response.body?.string() ?: ""
                        if (!response.isSuccessful) return@withContext Result.failure(IOException("HTTP ${response.code}: $bodyString"))
                        val text = JSONObject(bodyString).getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                        Result.success(text)
                    }
                }
                "OpenAI" -> {
                    val url = "https://api.openai.com/v1/chat/completions"
                    val requestBodyJson = JSONObject().apply {
                        put("model", model)
                        put("messages", JSONArray().apply {
                            put(JSONObject().apply {
                                put("role", "system")
                                put("content", systemInstruction)
                            })
                            put(JSONObject().apply {
                                put("role", "user")
                                put("content", prompt)
                            })
                        })
                    }
                    val request = Request.Builder()
                        .url(url)
                        .header("Authorization", "Bearer $apiKey")
                        .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                        .build()
                    client.newCall(request).execute().use { response ->
                        val bodyString = response.body?.string() ?: ""
                        if (!response.isSuccessful) return@withContext Result.failure(IOException("HTTP ${response.code}: $bodyString"))
                        val text = JSONObject(bodyString).getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")
                        Result.success(text)
                    }
                }
                "Claude" -> {
                    val url = "https://api.anthropic.com/v1/messages"
                    val requestBodyJson = JSONObject().apply {
                        put("model", model)
                        put("system", systemInstruction)
                        put("max_tokens", 4000)
                        put("messages", JSONArray().apply {
                            put(JSONObject().apply {
                                put("role", "user")
                                put("content", prompt)
                            })
                        })
                    }
                    val request = Request.Builder()
                        .url(url)
                        .header("x-api-key", apiKey)
                        .header("anthropic-version", "2023-06-01")
                        .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                        .build()
                    client.newCall(request).execute().use { response ->
                        val bodyString = response.body?.string() ?: ""
                        if (!response.isSuccessful) return@withContext Result.failure(IOException("HTTP ${response.code}: $bodyString"))
                        val text = JSONObject(bodyString).getJSONArray("content")
                            .getJSONObject(0)
                            .getString("text")
                        Result.success(text)
                    }
                }
                "OpenRouter" -> {
                    val url = "https://openrouter.ai/api/v1/chat/completions"
                    val requestBodyJson = JSONObject().apply {
                        put("model", model)
                        put("messages", JSONArray().apply {
                            put(JSONObject().apply {
                                put("role", "system")
                                put("content", systemInstruction)
                            })
                            put(JSONObject().apply {
                                put("role", "user")
                                put("content", prompt)
                            })
                        })
                    }
                    val request = Request.Builder()
                        .url(url)
                        .header("Authorization", "Bearer $apiKey")
                        .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                        .build()
                    client.newCall(request).execute().use { response ->
                        val bodyString = response.body?.string() ?: ""
                        if (!response.isSuccessful) return@withContext Result.failure(IOException("HTTP ${response.code}: $bodyString"))
                        val text = JSONObject(bodyString).getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")
                        Result.success(text)
                    }
                }
                else -> {
                    Result.success("Suggestions based on prompt: $prompt\n- Option 1: Introduce innovative institutional coverage\n- Option 2: Run an interactive media challenge\n- Option 3: Conduct executive interviews with leaders")
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
