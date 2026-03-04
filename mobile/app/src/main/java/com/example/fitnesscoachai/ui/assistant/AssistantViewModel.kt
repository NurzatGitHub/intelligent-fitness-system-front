package com.example.fitnesscoachai.ui.assistant

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AssistantViewModel : ViewModel() {

    private val _messages = MutableLiveData<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = "Привет! Я AI-ассистент. Чем помочь по тренировкам?",
                isUser = false
            )
        )
    )
    val messages: LiveData<List<ChatMessage>> = _messages

    private val _typing = MutableLiveData(false)
    val typing: LiveData<Boolean> = _typing

    fun sendUserMessage(text: String) {
        if (text.isBlank()) return

        val current = _messages.value.orEmpty().toMutableList()
        current.add(ChatMessage(text, isUser = true))
        _messages.value = current

        _typing.value = true
        viewModelScope.launch {
            delay(900)
            val reply = mockReply(text)
            val updated = _messages.value.orEmpty().toMutableList()
            updated.add(ChatMessage(reply, isUser = false))
            _messages.value = updated
            _typing.value = false
        }
    }

    private fun mockReply(userText: String): String {
        return "Понял. (Mock ответ) По вопросу: \"$userText\" — скоро подключим реальный backend и буду отвечать точнее."
    }
}

