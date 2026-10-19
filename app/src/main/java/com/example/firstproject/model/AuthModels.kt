package com.example.firstproject.model

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

// Shapes mirror the NestJS backend (src/auth/dto, src/user/dtos, src/profile/dto).
// Response fields are nullable because Gson ignores Kotlin nullability and the backend
// sends null (or omits) several of them.

/** Body of POST /auth/local/signin (SignInDto). */
data class SignInRequest(
    val email: String,
    val password: String
)

/** Response of POST /auth/local/signin and /auth/local/signup (AuthResponseDto). */
data class AuthResponse(
    val message: String?,
    val user: UserDto?,
    val tokens: Tokens?
)

data class Tokens(
    val accessToken: String?,
    val refreshToken: String?
)

data class UserDto(
    val id: Int?,
    val email: String?,
    @SerializedName("auth_provider") val authProvider: String?,
    val profile: ProfileDto?
)

data class ProfileDto(
    val id: Int?,
    val email: String?,
    val username: String?,
    val description: String?,
    val profileImageUrl: String?,
    val profileImageBase64: String?,
    val coverImageUrl: String?,
    val fcmToken: String?,
    val preferences: JsonElement?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?
)

/**
 * Error body produced by the backend's AllExceptionsFilter: {"statusCode": 403, "message": "Access denied"}.
 * `message` is a JsonElement because stock NestJS validation errors send it as an array of strings.
 */
data class ErrorResponse(
    val statusCode: Int?,
    val message: JsonElement?
)
