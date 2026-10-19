package com.vasudev.ai.master.core

data class ChatMessage(
    val id: Long,
    val role: String,
    val content: String,
    val imageUri: String? = null,
    val timestamp: Long = 0L,
    val tier: String = ""
)

data class SearchResult(
    val title: String,
    val url: String,
    val snippet: String
)
