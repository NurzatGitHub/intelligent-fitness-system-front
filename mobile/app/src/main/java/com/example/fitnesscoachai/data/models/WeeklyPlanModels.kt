package com.example.fitnesscoachai.data.models

data class WeeklyPlanDay(
    val day_key: String,
    val label: String,
    val type: String,
    val title: String,
    val duration_min: Int,
    val note: String
)

data class WeeklyPlanResponse(
    val title: String,
    val goal_summary: String,
    val days: List<WeeklyPlanDay>,
    val today_tip: String,
    val generated_at: String? = null
)