package com.example.screens.event

import android.app.Application
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.models.EventAIContentEntity
import com.example.services.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

sealed interface ContentDetailUiState {
    object Loading : ContentDetailUiState
    data class Success(val content: EventAIContentEntity) : ContentDetailUiState
    data class Error(val message: String) : ContentDetailUiState
}

class AIContentDetailsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val eventAIContentDao = database.eventAIContentDao()

    private val _uiState = MutableStateFlow<ContentDetailUiState>(ContentDetailUiState.Loading)
    val uiState: StateFlow<ContentDetailUiState> = _uiState.asStateFlow()

    fun loadContent(contentId: String) {
        viewModelScope.launch {
            _uiState.value = ContentDetailUiState.Loading
            try {
                val item = eventAIContentDao.getAIContentById(contentId)
                if (item != null) {
                    _uiState.value = ContentDetailUiState.Success(item)
                } else {
                    _uiState.value = ContentDetailUiState.Error("Content package not found.")
                }
            } catch (e: Exception) {
                _uiState.value = ContentDetailUiState.Error(e.localizedMessage ?: "Failed to load content details")
            }
        }
    }

    fun updateField(id: String, fieldName: String, newValue: String) {
        val currentState = _uiState.value
        if (currentState is ContentDetailUiState.Success) {
            val updated = when (fieldName) {
                "headline" -> currentState.content.copy(headline = newValue)
                "facebookPost" -> currentState.content.copy(facebookPost = newValue)
                "shortPost" -> currentState.content.copy(shortPost = newValue)
                "caption" -> currentState.content.copy(caption = newValue)
                "hashtags" -> currentState.content.copy(hashtags = newValue)
                "newsSummary" -> currentState.content.copy(newsSummary = newValue)
                "voiceOverScript" -> currentState.content.copy(voiceOverScript = newValue)
                "title" -> currentState.content.copy(title = newValue)
                else -> currentState.content
            }
            _uiState.value = ContentDetailUiState.Success(updated)
            viewModelScope.launch {
                eventAIContentDao.insertOrUpdateAIContent(updated)
            }
        }
    }

    fun deleteContent(id: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            eventAIContentDao.deleteAIContentById(id)
            onDeleted()
        }
    }

    fun duplicateContent(item: EventAIContentEntity, onDuplicated: (String) -> Unit) {
        viewModelScope.launch {
            val newId = java.util.UUID.randomUUID().toString()
            val duplicate = item.copy(
                id = newId,
                title = "Copy of ${item.title}",
                generationTime = System.currentTimeMillis()
            )
            eventAIContentDao.insertOrUpdateAIContent(duplicate)
            onDuplicated(newId)
        }
    }

    fun shareSection(context: Context, label: String, content: String) {
        try {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, label)
                putExtra(Intent.EXTRA_TEXT, content)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share $label"))
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to share: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportAsMarkdown(context: Context, item: EventAIContentEntity) {
        val mdText = """
            # ${item.title}
            
            ## Headline
            ${item.headline}
            
            ## Facebook Post
            ${item.facebookPost}
            
            ## Short Post (X/Telegram)
            ${item.shortPost}
            
            ## Image Caption
            ${item.caption}
            
            ## Hashtags
            ${item.hashtags}
            
            ## Official News Summary
            ${item.newsSummary}
            
            ## Voice-over Script
            ${item.voiceOverScript}
        """.trimIndent()
        writeFileAndShare(context, item.id, "AI_Generated_Package.md", mdText, "text/markdown")
    }

    fun exportAsTxt(context: Context, item: EventAIContentEntity) {
        val txtText = """
            === TITLE: ${item.title} ===
            
            === HEADLINE ===
            ${item.headline}
            
            === FACEBOOK POST ===
            ${item.facebookPost}
            
            === SHORT POST ===
            ${item.shortPost}
            
            === IMAGE CAPTION ===
            ${item.caption}
            
            === HASHTAGS ===
            ${item.hashtags}
            
            === NEWS SUMMARY ===
            ${item.newsSummary}
            
            === VOICE OVER SCRIPT ===
            ${item.voiceOverScript}
        """.trimIndent()
        writeFileAndShare(context, item.id, "AI_Generated_Package.txt", txtText, "text/plain")
    }

    fun exportAsJson(context: Context, item: EventAIContentEntity) {
        val json = JSONObject().apply {
            put("id", item.id)
            put("eventId", item.eventId)
            put("title", item.title)
            put("headline", item.headline)
            put("facebookPost", item.facebookPost)
            put("shortPost", item.shortPost)
            put("caption", item.caption)
            put("hashtags", item.hashtags)
            put("newsSummary", item.newsSummary)
            put("voiceOverScript", item.voiceOverScript)
            put("generationTime", item.generationTime)
            put("language", item.language)
            put("provider", item.provider)
            put("model", item.model)
        }
        writeFileAndShare(context, item.id, "AI_Generated_Package.json", json.toString(4), "application/json")
    }

    private fun writeFileAndShare(context: Context, id: String, filename: String, content: String, mimeType: String) {
        try {
            val exportDir = File(context.filesDir, "exports/$id")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            val file = File(exportDir, filename)
            file.writeText(content)

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Exported Package"))
            Toast.makeText(context, "$filename exported successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
