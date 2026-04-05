package com.example.fitnesscoachai.data.api

import com.example.fitnesscoachai.data.models.AuthResponse
import com.example.fitnesscoachai.data.models.ChatRequest
import com.example.fitnesscoachai.data.models.ChatResponse
import com.example.fitnesscoachai.data.models.ExerciseCategoryResponse
import com.example.fitnesscoachai.data.models.ExerciseDetailResponse
import com.example.fitnesscoachai.data.models.ExerciseListItemResponse
import com.example.fitnesscoachai.data.models.ExerciseSubcategoryResponse
import com.example.fitnesscoachai.data.models.GoogleLoginRequest
import com.example.fitnesscoachai.data.models.LoginRequest
import com.example.fitnesscoachai.data.models.RegisterRequest
import com.example.fitnesscoachai.data.models.UpdateProfileRequest
import com.example.fitnesscoachai.data.models.User
import com.example.fitnesscoachai.data.models.WeeklyPlanResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("api/users/login/")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/users/register/")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/users/google/")
    suspend fun google(@Body request: GoogleLoginRequest): Response<AuthResponse>

    @GET("api/users/me/")
    suspend fun getMe(
        @Header("Authorization") bearer: String
    ): Response<User>

    @PATCH("api/users/me/")
    suspend fun updateMe(
        @Header("Authorization") bearer: String,
        @Body body: UpdateProfileRequest
    ): Response<User>

    @POST("api/assistant/chat/")
    suspend fun assistantChat(
        @Header("Authorization") bearer: String,
        @Body body: ChatRequest
    ): Response<ChatResponse>

    @GET("api/assistant/weekly-plan/")
    suspend fun getWeeklyPlan(
        @Header("Authorization") bearer: String
    ): Response<WeeklyPlanResponse>

    @POST("api/assistant/weekly-plan/regenerate/")
    suspend fun regenerateWeeklyPlan(
        @Header("Authorization") bearer: String
    ): Response<WeeklyPlanResponse>

    @GET("api/exercises/categories/")
    suspend fun getExerciseCategories(
        @Header("Authorization") bearer: String
    ): Response<List<ExerciseCategoryResponse>>

    @GET("api/exercises/subcategories/")
    suspend fun getExerciseSubcategories(
        @Header("Authorization") bearer: String,
        @Query("category") categorySlug: String
    ): Response<List<ExerciseSubcategoryResponse>>

    @GET("api/exercises/")
    suspend fun getExercises(
        @Header("Authorization") bearer: String,
        @Query("category") categorySlug: String? = null,
        @Query("subcategory") subcategorySlug: String? = null,
        @Query("search") search: String? = null
    ): Response<List<ExerciseListItemResponse>>

    @GET("api/exercises/{slug}/")
    suspend fun getExerciseDetail(
        @Header("Authorization") bearer: String,
        @Path("slug") slug: String
    ): Response<ExerciseDetailResponse>
}