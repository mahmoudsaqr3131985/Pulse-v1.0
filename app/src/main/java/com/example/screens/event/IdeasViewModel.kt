package com.example.screens.event

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.models.WorkspaceEntity
import com.example.services.AppDatabase
import com.example.services.ai.AIHttpClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface IdeasUiState {
    object Idle : IdeasUiState
    object Loading : IdeasUiState
    data class Success(val ideas: String, val category: String) : IdeasUiState
    data class Error(val message: String) : IdeasUiState
}

class IdeasViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val workspaceDao = database.workspaceDao()

    val activeWorkspace: StateFlow<WorkspaceEntity?> = workspaceDao.getActiveWorkspace()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _uiState = MutableStateFlow<IdeasUiState>(IdeasUiState.Idle)
    val uiState: StateFlow<IdeasUiState> = _uiState.asStateFlow()

    fun generateIdeasForCategory(category: String) {
        val workspace = activeWorkspace.value
        if (workspace == null) {
            _uiState.value = IdeasUiState.Error("No active workspace selected. Please select or create a workspace first.")
            return
        }

        val provider = workspace.aiProvider ?: "Gemini"
        val model = workspace.aiModel ?: "gemini-3.5-flash"
        val apiKey = workspace.aiApiKey

        if (apiKey.isNullOrBlank()) {
            _uiState.value = IdeasUiState.Error("Missing API Key in Workspace AI settings.")
            return
        }

        viewModelScope.launch {
            _uiState.value = IdeasUiState.Loading
            try {
                val systemInstruction = "You are Pulse AI Ideas Generator, a creative institutional media consultant. Create highly professional, engaging, and localized media plan suggestions based on the category requested. Return the suggestions in clear markdown bullet points, with an encouraging and authoritative tone."
                val prompt = "Generate creative and strategic content/media ideas for the category: '$category' within the institutional workspace called '${workspace.name}' (type: ${workspace.type}). Include specific actions, target audiences, and title suggestions."
                
                val result = AIHttpClient.generateText(provider, apiKey, model, systemInstruction, prompt)
                result.onSuccess { text ->
                    _uiState.value = IdeasUiState.Success(text, category)
                }.onFailure { error ->
                    val msg = when {
                        error is java.net.UnknownHostException -> "Internet connection is unavailable."
                        error is java.net.SocketTimeoutException -> "AI Request timed out. Please try again."
                        else -> error.localizedMessage ?: "Failed to generate suggestions."
                    }
                    _uiState.value = IdeasUiState.Error(msg)
                }
            } catch (e: Exception) {
                _uiState.value = IdeasUiState.Error(e.localizedMessage ?: "Unexpected error")
            }
        }
    }
}
