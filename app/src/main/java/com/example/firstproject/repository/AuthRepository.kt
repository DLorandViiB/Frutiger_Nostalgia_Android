package com.example.firstproject.repository

import com.example.firstproject.model.AuthResponse
import com.example.firstproject.model.ErrorResponse
import com.example.firstproject.model.SignInRequest
import com.example.firstproject.network.ApiService
import com.example.firstproject.utils.SessionManager
import com.google.gson.Gson
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response

/** Non-2xx answer from the backend, with the message parsed from its error body (if any). */
class ApiException(val statusCode: Int, val serverMessage: String?) :
    Exception("HTTP $statusCode${serverMessage?.let { ": $it" } ?: ""}")

class AuthRepository(
    private val api: ApiService,
    private val sessionManager: SessionManager
) {
    private val gson = Gson()

    /** Signs in and, on success, stores the session. */
    suspend fun login(email: String, password: String): Result<AuthResponse> = runCatching {
        api.signIn(SignInRequest(email, password)).bodyOrThrow().also(::saveSession)
    }

    /** Creates the account. The backend returns tokens here too, so the user is signed in right away. */
    suspend fun signup(username: String, email: String, password: String): Result<AuthResponse> =
        runCatching {
            api.signUp(
                username = username.toTextPart(),
                email = email.toTextPart(),
                password = password.toTextPart()
            ).bodyOrThrow().also(::saveSession)
        }

    /** Best effort server-side logout (revokes the refresh token), then always clears the local session. */
    suspend fun logout() {
        if (sessionManager.isLoggedIn()) {
            runCatching { withTimeoutOrNull(LOGOUT_TIMEOUT_MS) { api.logout() } }
        }
        sessionManager.clearSession()
    }

    private fun saveSession(response: AuthResponse) {
        val accessToken = response.tokens?.accessToken
            ?: throw IllegalStateException("Response contained no access token")
        sessionManager.saveTokens(accessToken, response.tokens.refreshToken)
        val user = response.user
        sessionManager.saveUser(user?.id, user?.profile?.username, user?.email)
    }

    private fun Response<AuthResponse>.bodyOrThrow(): AuthResponse {
        val body = body()
        if (isSuccessful && body?.tokens?.accessToken != null) return body
        if (isSuccessful) throw IllegalStateException("Unexpected response from server")
        throw ApiException(code(), parseErrorMessage())
    }

    /** Reads {"statusCode", "message"}; message may be a string or an array of strings. */
    private fun Response<*>.parseErrorMessage(): String? = runCatching {
        val raw = errorBody()?.string().orEmpty()
        val message = gson.fromJson(raw, ErrorResponse::class.java)?.message ?: return@runCatching null
        when {
            message.isJsonArray -> message.asJsonArray.joinToString("\n") { it.asString }
            message.isJsonPrimitive -> message.asString
            else -> null
        }
    }.getOrNull()?.takeIf { it.isNotBlank() }

    private fun String.toTextPart() = toRequestBody(TEXT_PLAIN)

    private companion object {
        val TEXT_PLAIN = "text/plain".toMediaType()
        const val LOGOUT_TIMEOUT_MS = 3_000L
    }
}
