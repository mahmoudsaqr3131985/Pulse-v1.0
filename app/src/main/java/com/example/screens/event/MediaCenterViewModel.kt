package com.example.screens.event

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.models.EventAIContentEntity
import com.example.models.WorkspaceEntity
import com.example.services.AppDatabase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class MediaSortOrder {
    NEWEST, OLDEST, ALPHABETICAL
}

class MediaCenterViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val eventAIContentDao = database.eventAIContentDao()
    private val workspaceDao = database.workspaceDao()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedWorkspaceId = MutableStateFlow<String?>(null)
    val selectedWorkspaceId = _selectedWorkspaceId.asStateFlow()

    private val _filterFavoritesOnly = MutableStateFlow(false)
    val filterFavoritesOnly = _filterFavoritesOnly.asStateFlow()

    private val _sortOrder = MutableStateFlow(MediaSortOrder.NEWEST)
    val sortOrder = _sortOrder.asStateFlow()

    val workspaces: StateFlow<List<WorkspaceEntity>> = workspaceDao.getAllWorkspaces()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val aiContents: StateFlow<List<EventAIContentEntity>> = combine(
        eventAIContentDao.getAllAIContentsFlow(),
        _searchQuery,
        _selectedWorkspaceId,
        _filterFavoritesOnly,
        _sortOrder
    ) { list, search, wsId, favOnly, order ->
        var filtered = list

        // 1. Search Query Filter (Title, Headline, News Summary, or Hashtags)
        if (search.isNotBlank()) {
            filtered = filtered.filter {
                it.title.contains(search, ignoreCase = true) ||
                it.headline.contains(search, ignoreCase = true) ||
                it.newsSummary.contains(search, ignoreCase = true) ||
                it.hashtags.contains(search, ignoreCase = true)
            }
        }

        // 2. Workspace Filter
        if (wsId != null) {
            // Need to join or check event's workspace ID, but since EventAIContentEntity is tied to Event, 
            // we can retrieve events if needed or match workspace via context workspaceInfo name or id, or just check event workspace.
            // Let's resolve the workspaceId mapping:
            val eventsInWorkspace = database.eventDao().getEventsForWorkspace(wsId).firstOrNull() ?: emptyList()
            val eventIds = eventsInWorkspace.map { it.id }.toSet()
            filtered = filtered.filter { it.eventId in eventIds }
        }

        // 3. Favorites Filter
        if (favOnly) {
            filtered = filtered.filter { it.isFavorite }
        }

        // 4. Sorting
        when (order) {
            MediaSortOrder.NEWEST -> filtered.sortedByDescending { it.generationTime }
            MediaSortOrder.OLDEST -> filtered.sortedBy { it.generationTime }
            MediaSortOrder.ALPHABETICAL -> filtered.sortedBy { it.title.lowercase() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setWorkspaceFilter(wsId: String?) {
        _selectedWorkspaceId.value = wsId
    }

    fun setFilterFavoritesOnly(favOnly: Boolean) {
        _filterFavoritesOnly.value = favOnly
    }

    fun setSortOrder(order: MediaSortOrder) {
        _sortOrder.value = order
    }

    fun toggleFavorite(item: EventAIContentEntity) {
        viewModelScope.launch {
            eventAIContentDao.insertOrUpdateAIContent(
                item.copy(isFavorite = !item.isFavorite)
            )
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            eventAIContentDao.deleteAIContentById(id)
        }
    }

    fun duplicateItem(item: EventAIContentEntity) {
        viewModelScope.launch {
            val duplicate = item.copy(
                id = java.util.UUID.randomUUID().toString(),
                title = "Copy of ${item.title}",
                generationTime = System.currentTimeMillis()
            )
            eventAIContentDao.insertOrUpdateAIContent(duplicate)
        }
    }
}
