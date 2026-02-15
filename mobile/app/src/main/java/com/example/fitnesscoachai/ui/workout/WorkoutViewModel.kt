package com.example.fitnesscoachai.ui.workout

import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*

class WorkoutViewModel : ViewModel() {

    private val client = OkHttpClient.Builder().build()
    private val gson = Gson()
    private var ws: WebSocket? = null

    private val _workoutState = MutableStateFlow<WorkoutState>(WorkoutState.Idle)
    val workoutState: StateFlow<WorkoutState> = _workoutState

    // TODO: вынеси в одну конфигурацию, сейчас хардкод
    private val wsUrlDevice = "ws://192.168.0.11:8000/ws/analyze/"

    private var currentReps = 0

    fun connectWebSocket() {
        _workoutState.value = WorkoutState.Connecting

        val request = Request.Builder().url(wsUrlDevice).build()
        ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _workoutState.value = WorkoutState.Connected
                webSocket.send("""{"type":"ping"}""")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val res = runCatching { gson.fromJson(text, WsResponse::class.java) }.getOrNull()
                    ?: return

                // служебные
                if (res.type == "pong" || res.type == "connected") return
                if (res.type == "error") {
                    _workoutState.value = WorkoutState.Error(res.message ?: "Server error")
                    return
                }

                when (res.status) {
                    "SETUP" -> _workoutState.value = WorkoutState.Setup(res.hint ?: "Примите позицию push-up")
                    "ACTIVE" -> {
                        currentReps = res.rep_count ?: currentReps
                        _workoutState.value = WorkoutState.Active(res)
                    }
                    else -> {
                        // игнор
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _workoutState.value = WorkoutState.Error("WebSocket failed: ${t.message}")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _workoutState.value = WorkoutState.Idle
            }
        })
    }

    fun sendLandmarks(points: List<PosePoint>) {
        val payload = mapOf(
            "exercise" to "push_up",
            "ts" to (System.currentTimeMillis() / 1000),
            "points" to points
        )
        ws?.send(gson.toJson(payload))
    }

    fun disconnectWebSocket() {
        ws?.close(1000, "bye")
        ws = null
    }

    fun getCurrentReps(): Int = currentReps

    sealed class WorkoutState {
        object Idle : WorkoutState()
        object Connecting : WorkoutState()
        object Connected : WorkoutState()
        data class Setup(val hint: String) : WorkoutState()
        data class Active(val res: WsResponse) : WorkoutState()
        data class Info(val msg: String) : WorkoutState()
        data class Error(val message: String) : WorkoutState()
    }
}
