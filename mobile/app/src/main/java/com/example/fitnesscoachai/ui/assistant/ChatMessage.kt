package com.example.fitnesscoachai.ui.assistant

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timeMillis: Long = System.currentTimeMillis()
)

