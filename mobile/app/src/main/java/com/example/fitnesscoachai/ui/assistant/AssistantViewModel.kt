package com.example.fitnesscoachai.ui.assistant

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnesscoachai.data.api.RetrofitClient
import com.example.fitnesscoachai.data.models.ChatRequest
import kotlinx.coroutines.launch
import org.json.JSONObject

class AssistantViewModel : ViewModel() {

    private val api = RetrofitClient.apiService

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

    fun sendUserMessage(text: String, accessToken: String) {
        if (text.isBlank()) return

        val current = _messages.value.orEmpty().toMutableList()
        current.add(ChatMessage(text, isUser = true))
        _messages.value = current

        _typing.value = true

        viewModelScope.launch {
            try {
                val bearer = "Bearer $accessToken"
                val resp = api.assistantChat(bearer, ChatRequest(message = text))

                val updated = _messages.value.orEmpty().toMutableList()

                if (resp.isSuccessful) {
                    val reply = resp.body()?.reply?.trim()
                    updated.add(
                        ChatMessage(
                            text = reply ?: "Пустой ответ от сервера.",
                            isUser = false
                        )
                    )
                } else {
                    val errorMessage = try {
                        val raw = resp.errorBody()?.string().orEmpty()
                        if (raw.isNotBlank()) {
                            val obj = JSONObject(raw)
                            val detail = obj.optString("detail")
                            val error = obj.optString("error")

                            buildString {
                                if (detail.isNotBlank()) append(detail)
                                if (error.isNotBlank()) {
                                    if (isNotEmpty()) append("\n")
                                    append(error)
                                }
                                if (isBlank()) append("Ошибка сервера: ${resp.code()}")
                            }
                        } else {
                            "Ошибка сервера: ${resp.code()}"
                        }
                    } catch (e: Exception) {
                        "Ошибка сервера: ${resp.code()}"
                    }

                    updated.add(
                        ChatMessage(
                            text = errorMessage,
                            isUser = false
                        )
                    )
                }

                _messages.value = updated

            } catch (e: Exception) {
                val updated = _messages.value.orEmpty().toMutableList()
                updated.add(
                    ChatMessage(
                        text = "Не удалось подключиться к серверу. Проверь IP, backend и интернет.",
                        isUser = false
                    )
                )
                _messages.value = updated
            } finally {
                _typing.value = false
            }
        }
    }
}