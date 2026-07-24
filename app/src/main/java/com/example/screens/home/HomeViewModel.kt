package com.example.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.models.EventAIContentEntity
import com.example.models.EventEntity
import com.example.models.WorkspaceEntity
import com.example.services.AppDatabase
import kotlinx.coroutines.flow.*

data class HomeStats(
    val totalEvents: Int = 0,
    val mediaCount: Int = 0,
    val totalSizeBytes: Long = 0L
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val workspaceDao = database.workspaceDao()
    private val eventDao = database.eventDao()
    private val eventAIContentDao = database.eventAIContentDao()
    private val mediaDao = database.mediaDao()

    val activeWorkspace: StateFlow<WorkspaceEntity?> = workspaceDao.getActiveWorkspace()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentEvents: StateFlow<List<EventEntity>> = activeWorkspace.flatMapLatest { ws ->
        if (ws != null) {
            eventDao.getEventsForWorkspace(ws.id).map { it.take(3) }
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyGeneratedContents: StateFlow<List<EventAIContentEntity>> = eventAIContentDao.getAllAIContentsFlow()
        .map { it.take(3) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val homeStats: StateFlow<HomeStats> = activeWorkspace.flatMapLatest { ws ->
        if (ws != null) {
            eventDao.getEventsForWorkspace(ws.id).flatMapLatest { events ->
                // Collect media items for all events in this workspace
                val mediaFlows = events.map { mediaDao.getMediaForEvent(it.id) }
                if (mediaFlows.isEmpty()) {
                    flowOf(HomeStats(totalEvents = events.size, mediaCount = 0, totalSizeBytes = 0L))
                } else {
                    combine(mediaFlows) { lists ->
                        val allMedia = lists.flatMap { it }
                        HomeStats(
                            totalEvents = events.size,
                            mediaCount = allMedia.size,
                            totalSizeBytes = allMedia.sumOf { it.fileSize }
                        )
                    }
                }
            }
        } else {
            flowOf(HomeStats())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeStats())
}
