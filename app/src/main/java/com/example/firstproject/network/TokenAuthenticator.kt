package com.example.firstproject.network

import android.util.Log
import com.example.firstproject.model.Tokens
import com.example.firstproject.utils.SessionManager
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Call
import retrofit2.http.Header
import retrofit2.http.POST
import java.io.IOException

/** The refresh call lives on its own client (no AuthInterceptor/Authenticator), so it can't recurse. */
interface AuthRefreshService {
    /** RtGuard reads the refresh token from the Authorization header. Returns a new token pair. */
    @POST("auth/local/refresh")
    fun refreshTokens(@Header("Authorization") bearerRefreshToken: String): Call<Tokens>
}

/**
 * On a 401 for a request that carried our access token, swaps the refresh token for a new pair and
 * retries once. If the refresh is rejected the session is cleared and the 401 reaches the caller,
 * which should send the user back to login.
 */
class TokenAuthenticator(
    private val sessionManager: SessionManager,
    private val refreshService: AuthRefreshService
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val sentToken = response.request.header(HEADER)?.removePrefix(BEARER)?.trim()
        // Not an authenticated request, or the retry with a fresh token was rejected too
        if (sentToken.isNullOrBlank() || response.priorResponse != null) return null

        synchronized(this) {
            val current = sessionManager.fetchAccessToken() ?: return null
            // Another request refreshed while this one waited for the lock
            if (current != sentToken) return response.request.withAccessToken(current)

            val refreshToken = sessionManager.fetchRefreshToken()
            if (refreshToken.isNullOrBlank()) {
                sessionManager.clearSession()
                return null
            }
            val refresh = try {
                refreshService.refreshTokens("$BEARER$refreshToken").execute()
            } catch (e: IOException) {
                Log.w(TAG, "Token refresh failed", e)
                return null
            }
            val tokens = refresh.body()
            val newAccessToken = tokens?.accessToken
            if (!refresh.isSuccessful || newAccessToken.isNullOrBlank()) {
                // 401/403: the refresh token is expired or revoked. Other codes may be transient.
                if (refresh.code() == 401 || refresh.code() == 403) sessionManager.clearSession()
                return null
            }
            sessionManager.saveTokens(newAccessToken, tokens.refreshToken ?: refreshToken)
            return response.request.withAccessToken(newAccessToken)
        }
    }

    private fun Request.withAccessToken(token: String) =
        newBuilder().header(HEADER, "$BEARER$token").build()

    private companion object {
        const val TAG = "TokenAuthenticator"
        const val HEADER = "Authorization"
        const val BEARER = "Bearer "
    }
}
