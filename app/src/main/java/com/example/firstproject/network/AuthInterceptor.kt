package com.example.firstproject.network

import com.example.firstproject.utils.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

/** Adds "Authorization: Bearer <access token>" to every request while a session exists. */
class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = sessionManager.fetchAccessToken()
        if (token.isNullOrBlank() || request.header(HEADER) != null) {
            return chain.proceed(request)
        }
        return chain.proceed(
            request.newBuilder()
                .header(HEADER, "Bearer $token")
                .build()
        )
    }

    private companion object {
        const val HEADER = "Authorization"
    }
}
