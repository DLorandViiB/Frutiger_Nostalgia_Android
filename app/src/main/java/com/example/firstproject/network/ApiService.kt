package com.example.firstproject.network

import com.example.firstproject.model.AuthResponse
import com.example.firstproject.model.SignInRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

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
}
