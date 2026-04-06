package com.example.fitnesscoachai.data.models

data class WorkoutExerciseRequest(
    val exercise_slug: String?,
    val exercise_name: String?,
    val completed_reps: Int,
    val duration_sec: Int
)

data class WorkoutSessionRequest(
    val title: String,
    val total_duration_sec: Int,
    val total_reps: Int,
    val exercises: List<WorkoutExerciseRequest>
)