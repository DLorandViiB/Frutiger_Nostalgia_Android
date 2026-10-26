package com.example.firstproject.network

import com.example.firstproject.model.AuthResponse
import com.example.firstproject.model.CreateCustomScheduleRequest
import com.example.firstproject.model.CreateHabitRequest
import com.example.firstproject.model.CreateRecurringScheduleRequest
import com.example.firstproject.model.HabitCategory
import com.example.firstproject.model.HabitResponse
import com.example.firstproject.model.ScheduleResponse
import com.example.firstproject.model.SignInRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface ApiService {

    @POST("auth/local/signin")
    suspend fun signIn(@Body request: SignInRequest): Response<AuthResponse>

    /** multipart/form-data, as declared by the backend; `profileImage` is optional and skipped when null. */
    @Multipart
    @POST("auth/local/signup")
    suspend fun signUp(
        @Part("username") username: RequestBody,
        @Part("email") email: RequestBody,
        @Part("password") password: RequestBody,
        @Part profileImage: MultipartBody.Part? = null
    ): Response<AuthResponse>

    /** Invalidates the stored refresh token on the server. Requires the access token. */
    @POST("auth/local/logout")
    suspend fun logout(): Response<Unit>

    /**
     * Schedules the user owns or participates in on [date] (YYYY-MM-DD). The backend matches it
     * against each schedule's stored calendar day, so pass the device's local date for "today".
     */
    @GET("schedule/day")
    suspend fun getSchedulesByDay(@Query("date") date: String): Response<List<ScheduleResponse>>

    /** Categories seeded by the backend on boot; there is no endpoint to create them. */
    @GET("habit/categories")
    suspend fun getHabitCategories(): Response<List<HabitCategory>>

    /** The current user's habits. */
    @GET("habit")
    suspend fun getHabits(): Response<List<HabitResponse>>

    @POST("habit")
    suspend fun createHabit(@Body request: CreateHabitRequest): Response<HabitResponse>

    /** Returns every generated occurrence; can be empty if no day in the range matches the pattern. */
    @POST("schedule/recurring")
    suspend fun createRecurringSchedule(
        @Body request: CreateRecurringScheduleRequest
    ): Response<List<ScheduleResponse>>

    @POST("schedule/custom")
    suspend fun createCustomSchedule(@Body request: CreateCustomScheduleRequest): Response<ScheduleResponse>
}
