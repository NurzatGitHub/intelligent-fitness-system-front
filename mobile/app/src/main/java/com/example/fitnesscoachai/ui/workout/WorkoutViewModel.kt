package com.example.fitnesscoachai.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnesscoachai.ui.camera.CameraViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WorkoutViewModel : ViewModel() {

    private val _workoutState = MutableStateFlow<WorkoutState>(WorkoutState.Idle)
    val workoutState: StateFlow<WorkoutState> = _workoutState

    private val cameraViewModel = CameraViewModel()
    private var currentReps = 0

    init {
        // Observe camera view model state
        viewModelScope.launch {
            cameraViewModel.analysisState.collect { state ->
                when (state) {
                    is CameraViewModel.AnalysisState.Connected -> {
                        _workoutState.value = WorkoutState.Connected
                    }
                    is CameraViewModel.AnalysisState.AnalysisReceived -> {
                        currentReps = state.result.count
                        _workoutState.value = WorkoutState.AnalysisReceived(state.result)
                    }
                    is CameraViewModel.AnalysisState.Error -> {
                        _workoutState.value = WorkoutState.Error(state.message)
                    }
                    else -> {}
                }
            }
        }
    }

    fun connectWebSocket() {
        cameraViewModel.connectWebSocket()
    }

    fun disconnectWebSocket() {
        cameraViewModel.disconnectWebSocket()
    }

    fun getCurrentReps(): Int = currentReps

    sealed class WorkoutState {
        object Idle : WorkoutState()
        object Connected : WorkoutState()
        data class AnalysisReceived(val result: com.example.fitnesscoachai.ui.camera.AnalysisResult) : WorkoutState()
        data class Error(val message: String) : WorkoutState()
    }
}
