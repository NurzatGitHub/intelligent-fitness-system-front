package com.example.fitnesscoachai.data.models

data class User(
    val id: Int,
    val email: String,
    val username: String,
    val age: Int? = null,
    val weight: Float? = null,
    val height: Float? = null,
    val fitness_level: String = "beginner",
    val goal: String = "",
    val limitations: String = "",
    val frequency: String = "",
)

data class LoginRequest(
    val email: String,
    val password: String,
)

// ✅ Новая модель для регистрации — включает все данные онбординга
data class RegisterRequest(
    val email: String,
    val password: String,
    val age: Int?,
    val height: Float?,
    val weight: Float?,
    val fitness_level: String,
    val goal: String,
    val limitations: String,
    val frequency: String,
)

data class AuthResponse(
    val user: User,
    val refresh: String,
    val access: String,
    val is_new_user: Boolean = false
)
data class GoogleLoginRequest(val id_token: String)