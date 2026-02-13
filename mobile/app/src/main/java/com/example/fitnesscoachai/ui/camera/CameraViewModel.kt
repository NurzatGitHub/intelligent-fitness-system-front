package com.example.fitnesscoachai.ui.camera

import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*

data class AnalysisResult(
    val type: String? = null,
    val exercise: String = "",
    val count: Int = 0,
    val errors: List<String> = emptyList(),
    val feedback: String = "",
    val score: Int = 0
)

class CameraViewModel : ViewModel() {

    private val client = OkHttpClient.Builder().build()
    private val gson = Gson()

    private var webSocket: WebSocket? = null

    private val _analysisState = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val analysisState: StateFlow<AnalysisState> = _analysisState

    private val wsUrlEmulator = "ws://10.0.2.2:8000/ws/analyze/"
    private val wsUrlDevice = "ws://192.168.0.14:8000/ws/analyze/"

    fun connectWebSocket(useEmulator: Boolean = false) {
        _analysisState.value = AnalysisState.Connecting

        val url = if (useEmulator) wsUrlEmulator else wsUrlDevice
        _analysisState.value = AnalysisState.Info("WS URL: $url")

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(ws: WebSocket, response: Response) {
                _analysisState.value = AnalysisState.Connected

                ws.send("""{"type":"ping"}""")
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val obj = gson.fromJson(text, JsonObject::class.java)

                    val type = when {
                        obj.has("type") -> obj.get("type").asString
                        obj.has("status") -> obj.get("status").asString
                        else -> "unknown"
                    }

                    when (type) {
                        "connected" -> {
                            _analysisState.value = AnalysisState.Info("Server connected")
                        }
                        "pong" -> {
                            _analysisState.value = AnalysisState.Info("Pong received")
                            ws.send("""{"type":"test","client":"android"}""")
                        }
                        "result" -> {
                            val result = gson.fromJson(text, AnalysisResult::class.java)
                            _analysisState.value = AnalysisState.AnalysisReceived(result)
                        }
                        "error" -> {
                            val msg = if (obj.has("message")) obj.get("message").asString else "error"
                            _analysisState.value = AnalysisState.Error("Server error: $msg")
                        }
                        else -> {
                            _analysisState.value = AnalysisState.Info("WS message: $text")
                        }
                    }

                } catch (e: Exception) {
                    _analysisState.value = AnalysisState.Error("Parse error: ${e.message}")
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                _analysisState.value = AnalysisState.Error("WebSocket failed: ${t.message}")
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                _analysisState.value = AnalysisState.Disconnected
            }
        })
    }

    fun disconnectWebSocket() {
        webSocket?.close(1000, "Normal closure")
        webSocket = null
        _analysisState.value = AnalysisState.Disconnected
    }

    sealed class AnalysisState {
        object Idle : AnalysisState()
        object Connecting : AnalysisState()
        object Connected : AnalysisState()
        data class Info(val message: String) : AnalysisState()
        data class AnalysisReceived(val result: AnalysisResult) : AnalysisState()
        data class Error(val message: String) : AnalysisState()
        object Disconnected : AnalysisState()
    }
}