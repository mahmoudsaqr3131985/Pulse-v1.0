package com.example.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "event_ai_contents")
data class EventAIContentEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val eventId: String,
    val title: String,
    val headline: String,
    val facebookPost: String,
    val shortPost: String,
    val caption: String,
    val hashtags: String,
    val newsSummary: String,
    val voiceOverScript: String,
    val generationTime: Long = System.currentTimeMillis(),
    val language: String,
    val provider: String,
    val model: String,
    val isFavorite: Boolean = false
)
