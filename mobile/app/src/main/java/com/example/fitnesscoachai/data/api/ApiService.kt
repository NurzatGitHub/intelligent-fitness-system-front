package com.example.fitnesscoachai.data.api

import com.example.fitnesscoachai.data.models.AuthResponse
import com.example.fitnesscoachai.data.models.GoogleLoginRequest
import com.example.fitnesscoachai.data.models.LoginRequest
import com.example.fitnesscoachai.data.models.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("api/users/login/")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/users/register/")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/users/google/")
    suspend fun google(@Body request: GoogleLoginRequest): Response<AuthResponse>
}