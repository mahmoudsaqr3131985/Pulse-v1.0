package com.example.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "ai_request_history")
data class AIRequestHistoryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val workspaceId: String,
    val date: Long = System.currentTimeMillis(),
    val provider: String,
    val model: String,
    val operation: String,
    val durationMs: Long = 0L,
    val status: String // "Success", "Failed", "Cancelled"
)
