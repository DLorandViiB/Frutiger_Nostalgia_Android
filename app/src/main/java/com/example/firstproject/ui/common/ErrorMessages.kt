package com.example.firstproject.ui.common

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.firstproject.R
import com.example.firstproject.repository.ApiException
import java.io.IOException
import java.net.SocketTimeoutException

/** The access token was rejected and TokenAuthenticator could not refresh it. */
val Throwable.isSessionExpired: Boolean
    get() = this is ApiException && statusCode == 401

/**
 * Readable message for a failed data request (habits/schedules). The auth screens keep their own
 * mapping in AuthViewModel because 401/403 mean "wrong credentials" there.
 */
fun Throwable.toUserMessage(context: Context): String = when (this) {
    is ApiException -> when {
        // ValidationPipe failures only say "Bad Request Exception"; service checks give a real reason
        statusCode == 400 -> serverMessage
            ?.takeUnless { it.equals(GENERIC_BAD_REQUEST, ignoreCase = true) }
            ?.let { context.getString(R.string.error_rejected_with_reason, it) }
            ?: context.getString(R.string.error_request_rejected)
        statusCode == 404 -> serverMessage ?: context.getString(R.string.error_not_found)
        statusCode == 429 -> context.getString(R.string.error_too_many_requests)
        statusCode >= 500 -> context.getString(R.string.error_server, statusCode)
        else -> context.getString(R.string.error_unknown_http, statusCode)
    }
    is SocketTimeoutException -> context.getString(R.string.error_timeout)
    is IOException -> context.getString(R.string.error_no_connection)
    else -> context.getString(R.string.error_unexpected)
}

/** Sends the user back to login (clearing the back stack) and explains why. */
fun Fragment.navigateToLoginAfterSessionExpired() {
    val navController = findNavController()
    if (navController.currentDestination?.id != R.id.loginFragment) {
        navController.navigate(R.id.action_global_loginFragment)
    }
    showAeroSnackbar(getString(R.string.session_expired), isError = true)
}

private const val GENERIC_BAD_REQUEST = "Bad Request Exception"
