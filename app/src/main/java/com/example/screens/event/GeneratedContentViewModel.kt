package com.example.screens.event
import com.example.utils.EncryptionUtils
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.models.AIContext
import com.example.models.EventAIContentEntity
import com.example.models.GeneratedMediaPackage
import com.example.services.AppDatabase
import com.example.services.ai.AIAdapterFactory
import com.example.services.ai.AIHttpClient
import com.example.services.ai.ContextEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

sealed interface AIContentUiState {
    object Idle : AIContentUiState
    object Loading : AIContentUiState
    data class Success(val mediaPackage: GeneratedMediaPackage) : AIContentUiState
    data class Error(val message: String) : AIContentUiState
}

class GeneratedContentViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val eventDao = database.eventDao()
    private val mediaDao = database.mediaDao()
    private val workspaceDao = database.workspaceDao()
    private val eventAIContentDao = database.eventAIContentDao()

    private val _uiState = MutableStateFlow<AIContentUiState>(AIContentUiState.Idle)
    val uiState: StateFlow<AIContentUiState> = _uiState.asStateFlow()

    private val _isRegeneratingSection = MutableStateFlow<String?>(null)
    val isRegeneratingSection: StateFlow<String?> = _isRegeneratingSection.asStateFlow()

    private var activeJob: Job? = null

    var currentContext: AIContext? = null
        private set

    var existingContentId: String? = null
        private set

    fun cancelGeneration() {
        activeJob?.cancel()
        _uiState.value = AIContentUiState.Idle
    }

    fun loadOrGenerate(eventId: String) {
        viewModelScope.launch {
            _uiState.value = AIContentUiState.Loading
            try {
                // Pre-fetch context
                val event = eventDao.getEventById(eventId) ?: return@launch
                val workspace = workspaceDao.getWorkspaceById(event.workspaceId) ?: return@launch
                val mediaItems = mediaDao.getMediaForEventSync(eventId)
                val aiContext = ContextEngine.getInstance().buildContext(
                    context = getApplication(),
                    workspace = workspace,
                    event = event,
                    mediaItems = mediaItems
                )
                currentContext = aiContext

                val existing = eventAIContentDao.getAIContentForEvent(eventId)
                if (existing != null) {
                    existingContentId = existing.id
                    val mediaPkg = GeneratedMediaPackage(
                        headline = existing.headline,
                        facebookPost = existing.facebookPost,
                        shortPost = existing.shortPost,
                        caption = existing.caption,
                        hashtags = existing.hashtags,
                        newsSummary = existing.newsSummary,
                        voiceOverScript = existing.voiceOverScript
                    )
                    _uiState.value = AIContentUiState.Success(mediaPkg)
                } else {
                    existingContentId = null
                    generateAll(eventId)
                }
            } catch (e: Exception) {
                _uiState.value = AIContentUiState.Error(e.localizedMessage ?: "Unknown error loading content")
            }
        }
    }

    fun generateAll(eventId: String) {
        activeJob?.cancel()
        existingContentId = null // Generating a new package stores a new row!
        activeJob = viewModelScope.launch {
            _uiState.value = AIContentUiState.Loading
            
            val event = eventDao.getEventById(eventId)
            if (event == null) {
                _uiState.value = AIContentUiState.Error("Event not found.")
                return@launch
            }
            val workspace = workspaceDao.getWorkspaceById(event.workspaceId)
            if (workspace == null) {
                _uiState.value = AIContentUiState.Error("Workspace not found.")
                return@launch
            }
            val mediaItems = mediaDao.getMediaForEventSync(eventId)

            val packageDir = File(getApplication<Application>().filesDir, "packages/$eventId")
            
            val validation = ContextEngine.getInstance().validateContext(workspace, event, packageDir)
            if (!validation.isValid) {
                _uiState.value = AIContentUiState.Error("Context Validation Failed: ${validation.errors.joinToString(", ")}")
                return@launch
            }

            val aiContext = ContextEngine.getInstance().buildContext(
                context = getApplication(),
                workspace = workspace,
                event = event,
                mediaItems = mediaItems
            )
            currentContext = aiContext

            val providerName = aiContext.selectedAIProvider
            val modelName = aiContext.selectedAIModel
            val apiKey = EncryptionUtils.decrypt(workspace.aiApiKey)

            if (providerName.isBlank() || providerName.lowercase() == "none") {
                _uiState.value = AIContentUiState.Error("Selected AI Provider is unavailable. Please configure AI settings.")
                return@launch
            }

            if (apiKey.isNullOrBlank()) {
                _uiState.value = AIContentUiState.Error("Missing API Key in Workspace AI settings.")
                return@launch
            }

            val provider = AIAdapterFactory.getProvider(providerName)
            
            val result = provider.generateMediaPackage(apiKey, modelName, aiContext)
            
            result.onSuccess { mediaPkg ->
                _uiState.value = AIContentUiState.Success(mediaPkg)
                saveContentToDb(eventId, aiContext, mediaPkg)
            }.onFailure { error ->
                val friendlyError = when {
                    error is java.net.UnknownHostException -> "Internet connection is unavailable."
                    error is java.net.SocketTimeoutException -> "AI Request timed out. Please try again."
                    error.message?.contains("API key", ignoreCase = true) == true -> "Missing API Key in Workspace AI settings."
                    else -> error.localizedMessage ?: "Received an invalid or empty response from AI."
                }
                _uiState.value = AIContentUiState.Error(friendlyError)
            }
        }
    }

    fun updateField(eventId: String, updatedPackage: GeneratedMediaPackage) {
        val state = _uiState.value
        if (state is AIContentUiState.Success) {
            _uiState.value = AIContentUiState.Success(updatedPackage)
            currentContext?.let { ctx ->
                viewModelScope.launch {
                    saveContentToDb(eventId, ctx, updatedPackage)
                }
            }
        }
    }

    fun saveDraft(eventId: String, mediaPkg: GeneratedMediaPackage) {
        viewModelScope.launch {
            currentContext?.let { ctx ->
                saveContentToDb(eventId, ctx, mediaPkg)
            }
        }
    }

    private suspend fun saveContentToDb(eventId: String, ctx: AIContext, pkg: GeneratedMediaPackage) {
        val idToUse = existingContentId ?: java.util.UUID.randomUUID().toString()
        existingContentId = idToUse // Keep the same ID for further updates
        val entity = EventAIContentEntity(
            id = idToUse,
            eventId = eventId,
            title = ctx.eventInfo.title,
            headline = pkg.headline,
            facebookPost = pkg.facebookPost,
            shortPost = pkg.shortPost,
            caption = pkg.caption,
            hashtags = pkg.hashtags,
            newsSummary = pkg.newsSummary,
            voiceOverScript = pkg.voiceOverScript,
            language = ctx.language,
            provider = ctx.selectedAIProvider,
            model = ctx.selectedAIModel
        )
        eventAIContentDao.insertOrUpdateAIContent(entity)
    }

    fun regenerateSection(eventId: String, sectionKey: String) {
        val state = _uiState.value
        if (state !is AIContentUiState.Success) return

        val ctx = currentContext ?: return
        
        _isRegeneratingSection.value = sectionKey

        viewModelScope.launch {
            try {
                val workspace = workspaceDao.getWorkspaceById(ctx.workspaceInfo.id)
                val apiKey = EncryptionUtils.decrypt(workspace?.aiApiKey)
                if (apiKey.isNullOrBlank()) {
                    _isRegeneratingSection.value = null
                    return@launch
                }

                val providerName = ctx.selectedAIProvider
                val modelName = ctx.selectedAIModel
                val apiModel = AIHttpClient.mapModelToApiName(providerName, modelName)

                val prompt = """
                    Based on the following event context, regenerate ONLY the section '$sectionKey'.
                    
                    Title: ${ctx.eventInfo.title}
                    Description: ${ctx.eventInfo.description}
                    Location: ${ctx.eventInfo.location}
                    Language: ${ctx.language}
                    
                    Return a JSON object with exactly ONE key '$sectionKey' containing the updated text. 
                    Do not return any other fields.
                """.trimIndent()

                val responseTextResult = if (providerName == "Gemini") {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$apiModel:generateContent?key=$apiKey"
                    val requestBodyJson = JSONObject().apply {
                        put("contents", JSONArray().apply {
                            put(JSONObject().apply {
                                put("parts", JSONArray().apply {
                                    put(JSONObject().apply { put("text", prompt) })
                                })
                            })
                        })
                        put("generationConfig", JSONObject().apply {
                            put("responseMimeType", "application/json")
                        })
                    }
                    val request = Request.Builder()
                        .url(url)
                        .post(requestBodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                        .build()
                    executeRawRequest(request, "Gemini")
                } else if (providerName == "Claude") {
                    val url = "https://api.anthropic.com/v1/messages"
                    val requestBodyJson = JSONObject().apply {
                        put("model", apiModel)
                        put("max_tokens", 2048)
                        val messagesArray = JSONArray().apply {
                            put(JSONObject().apply {
                                put("role", "user")
                                put("content", prompt)
                            })
                        }
                        put("messages", messagesArray)
                    }
                    val request = Request.Builder()
                        .url(url)
                        .header("x-api-key", apiKey)
                        .header("anthropic-version", "2023-06-01")
                        .header("Content-Type", "application/json")
                        .post(requestBodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                        .build()
                    executeRawRequest(request, "Claude")
                } else {
                    val baseUrl = if (providerName == "OpenRouter") "https://openrouter.ai/api/v1/chat/completions" else "https://api.openai.com/v1/chat/completions"
                    val requestBodyJson = JSONObject().apply {
                        put("model", apiModel)
                        val messagesArray = JSONArray().apply {
                            put(JSONObject().apply {
                                put("role", "user")
                                put("content", prompt)
                            })
                        }
                        put("messages", messagesArray)
                        put("response_format", JSONObject().apply { put("type", "json_object") })
                    }
                    val request = Request.Builder()
                        .url(baseUrl)
                        .header("Authorization", "Bearer $apiKey")
                        .header("Content-Type", "application/json")
                        .post(requestBodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                        .build()
                    executeRawRequest(request, "OpenAI")
                }

                responseTextResult.onSuccess { rawJson ->
                    var cleanJson = rawJson.trim()
                    if (cleanJson.contains("```json")) {
                        cleanJson = cleanJson.substringAfter("```json").substringBefore("```")
                    } else if (cleanJson.contains("```")) {
                        cleanJson = cleanJson.substringAfter("```").substringBefore("```")
                    }
                    cleanJson = cleanJson.trim()
                    
                    val obj = JSONObject(cleanJson)
                    val newText = obj.optString(sectionKey, "").trim()
                    if (newText.isNotBlank()) {
                        val currentPkg = state.mediaPackage
                        val updatedPkg = when (sectionKey) {
                            "headline" -> currentPkg.copy(headline = newText)
                            "facebookPost" -> currentPkg.copy(facebookPost = newText)
                            "shortPost" -> currentPkg.copy(shortPost = newText)
                            "caption" -> currentPkg.copy(caption = newText)
                            "hashtags" -> currentPkg.copy(hashtags = newText)
                            "newsSummary" -> currentPkg.copy(newsSummary = newText)
                            "voiceOverScript" -> currentPkg.copy(voiceOverScript = newText)
                            else -> currentPkg
                        }
                        _uiState.value = AIContentUiState.Success(updatedPkg)
                        saveContentToDb(eventId, ctx, updatedPkg)
                    }
                }
            } catch (e: Exception) {
                // Ignore silent failure
            } finally {
                _isRegeneratingSection.value = null
            }
        }
    }

    private suspend fun executeRawRequest(request: Request, type: String): Result<String> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            AIHttpClient.client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP Error ${response.code}: $body"))
                }
                val rawText = when (type) {
                    "Gemini" -> {
                        JSONObject(body).getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                    }
                    "Claude" -> {
                        JSONObject(body).getJSONArray("content")
                            .getJSONObject(0)
                            .getString("text")
                    }
                    else -> {
                        JSONObject(body).getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")
                    }
                }
                Result.success(rawText)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
