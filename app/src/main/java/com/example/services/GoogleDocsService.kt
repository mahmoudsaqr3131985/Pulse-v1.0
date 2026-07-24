package com.example.services

/**
 * Service contract prepared for future Google Docs API integration.
 * DO NOT implement in Phase 1.
 */
interface GoogleDocsService {
    suspend fun createPressReleaseDoc(title: String, content: String): Result<String>
    suspend fun exportTranscriptToDoc(eventTitle: String, transcript: String): Result<String>
}
