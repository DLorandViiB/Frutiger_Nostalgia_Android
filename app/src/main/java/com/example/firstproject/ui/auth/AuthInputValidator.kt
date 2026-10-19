package com.example.firstproject.ui.auth

import android.util.Patterns
import androidx.annotation.StringRes
import com.example.firstproject.R

/**
 * Client-side checks mirroring the backend DTOs (AuthDto / SignInDto): every field is a required
 * string, email must be an email, and profile.username is a varchar(100). The backend has no
 * password length rule. Its validation errors only come back as "Bad Request Exception", so
 * catching these locally is what gives the user a useful message.
 * Each function returns an error string resource, or null when the value is valid.
 */
object AuthInputValidator {

    const val USERNAME_MAX_LENGTH = 100

    @StringRes
    fun validateEmail(email: String): Int? = when {
        email.isBlank() -> R.string.error_required_email
        !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> R.string.error_invalid_email
        else -> null
    }

    @StringRes
    fun validatePassword(password: String): Int? =
        if (password.isEmpty()) R.string.error_required_password else null

    @StringRes
    fun validateUsername(username: String): Int? = when {
        username.isBlank() -> R.string.error_required_username
        username.length > USERNAME_MAX_LENGTH -> R.string.error_username_too_long
        else -> null
    }
}
