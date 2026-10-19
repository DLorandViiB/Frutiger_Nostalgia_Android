package com.example.firstproject.utils

import android.content.Context
import androidx.core.content.edit

/** Persists the auth tokens and a little user info in SharedPreferences. */
class SessionManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveTokens(accessToken: String, refreshToken: String?) {
        prefs.edit {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
        }
    }

    fun saveUser(userId: Int?, username: String?, email: String?) {
        prefs.edit {
            if (userId != null) putInt(KEY_USER_ID, userId) else remove(KEY_USER_ID)
            putString(KEY_USERNAME, username)
            putString(KEY_EMAIL, email)
        }
    }

    fun fetchAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun fetchRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun fetchUsername(): String? = prefs.getString(KEY_USERNAME, null)

    fun fetchEmail(): String? = prefs.getString(KEY_EMAIL, null)

    fun isLoggedIn(): Boolean = !fetchAccessToken().isNullOrBlank()

    fun clearSession() {
        prefs.edit { clear() }
    }

    private companion object {
        const val PREFS_NAME = "session_prefs"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_USER_ID = "user_id"
        const val KEY_USERNAME = "username"
        const val KEY_EMAIL = "email"
    }
}
