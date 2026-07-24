package com.example.screens.event

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.models.EventAIContentEntity
import com.example.models.EventEntity
import com.example.services.AppDatabase
import kotlinx.coroutines.flow.*

data class SearchResult(
    val events: List<EventEntity> = emptyList(),
    val contents: List<EventAIContentEntity> = emptyList(),
    val matchingCategories: List<String> = emptyList()
)

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val eventDao = database.eventDao()
    private val eventAIContentDao = database.eventAIContentDao()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val categories = listOf(
        "News", "Student activities", "Awareness campaigns", "Veterinary tips",
        "Podcast ideas", "Interview ideas", "Reel ideas", "Short video ideas",
        "Upcoming occasions", "Weekly content ideas", "Monthly content plan"
    )

    val searchResults: StateFlow<SearchResult> = combine(
        _searchQuery,
        // Since we want reactive search, we can fetch all and filter or construct custom streams
        database.workspaceDao().getActiveWorkspace().flatMapLatest { ws ->
            if (ws != null) eventDao.getEventsForWorkspace(ws.id) else flowOf(emptyList())
        },
        eventAIContentDao.getAllAIContentsFlow()
    ) { query, events, contents ->
        if (query.isBlank()) {
            SearchResult()
        } else {
            val q = query.trim().lowercase()
            
            val filteredEvents = events.filter {
                it.title.lowercase().contains(q) ||
                (it.description?.lowercase()?.contains(q) == true) ||
                (it.location?.lowercase()?.contains(q) == true)
            }

            val filteredContents = contents.filter {
                it.title.lowercase().contains(q) ||
                it.headline.lowercase().contains(q) ||
                it.facebookPost.lowercase().contains(q) ||
                it.newsSummary.lowercase().contains(q)
            }

            val filteredCategories = categories.filter {
                it.lowercase().contains(q)
            }

            SearchResult(
                events = filteredEvents,
                contents = filteredContents,
                matchingCategories = filteredCategories
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchResult())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}
