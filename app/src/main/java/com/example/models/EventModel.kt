package com.example.models

/**
 * Model prepared for Phase 2+ Event tracking and Media management.
 * Not active in Phase 1.
 */
data class EventModel(
    val id: String,
    val title: String,
    val description: String,
    val dateTimestamp: Long,
    val location: String,
    val mediaCount: Int = 0,
    val status: EventStatus = EventStatus.UPCOMING
)

enum class EventStatus {
    UPCOMING,
    IN_PROGRESS,
    COMPLETED,
    ARCHIVED
}
