package com.example.fitnesscoachai.data.models

data class UpdateProfileRequest(
    val age: Int? = null,
    val height: Float? = null,
    val weight: Float? = null,
    val fitness_level: String? = null,
    val goal: String? = null,
    val limitations: String? = null,
    val frequency: String? = null
)