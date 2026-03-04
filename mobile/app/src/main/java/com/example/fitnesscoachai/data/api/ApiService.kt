package com.example.fitnesscoachai.data.api

import com.example.fitnesscoachai.data.models.AuthResponse
import com.example.fitnesscoachai.data.models.GoogleLoginRequest
import com.example.fitnesscoachai.data.models.LoginRequest
import com.example.fitnesscoachai.data.models.RegisterRequest
import com.example.fitnesscoachai.data.models.UpdateProfileRequest
import com.example.fitnesscoachai.data.models.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST

interface ApiService {

    @POST("api/users/login/")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/users/register/")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/users/google/")
    suspend fun google(@Body request: GoogleLoginRequest): Response<AuthResponse>

    @PATCH("api/users/me/")
    suspend fun updateMe(
        @Header("Authorization") bearer: String,
        @Body body: UpdateProfileRequest
    ): Response<User>
}