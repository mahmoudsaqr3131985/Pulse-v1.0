package com.example.services

import com.example.models.EventEntity
import kotlinx.coroutines.flow.Flow

class EventRepository(private val eventDao: EventDao) {

    fun getEventsForWorkspace(workspaceId: String): Flow<List<EventEntity>> {
        return eventDao.getEventsForWorkspace(workspaceId)
    }

    suspend fun getEventById(id: String): EventEntity? {
        return eventDao.getEventById(id)
    }

    suspend fun saveEvent(event: EventEntity) {
        eventDao.insertOrUpdateEvent(event)
    }

    suspend fun deleteEvent(event: EventEntity) {
        eventDao.deleteEvent(event)
    }

    suspend fun deleteEventById(id: String) {
        eventDao.deleteEventById(id)
    }
}
