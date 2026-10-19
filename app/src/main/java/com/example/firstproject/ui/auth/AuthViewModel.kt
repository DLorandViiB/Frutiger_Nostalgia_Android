package com.example.firstproject.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.firstproject.R
import com.example.firstproject.model.AuthResponse
import com.example.firstproject.network.RetrofitClient
import com.example.firstproject.repository.ApiException
import com.example.firstproject.repository.AuthRepository
import com.example.firstproject.utils.SessionManager
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val username: String?, val isNewAccount: Boolean) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository(
        RetrofitClient.getApiService(application),
        SessionManager(application)
    )

    private val _uiState = MutableLiveData<AuthUiState>(AuthUiState.Idle)
    val uiState: LiveData<AuthUiState> = _uiState

    fun login(email: String, password: String) {
        if (_uiState.value == AuthUiState.Loading) return
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            _uiState.value = repository.login(email, password).toUiState(isNewAccount = false)
        }
    }

    fun signup(username: String, email: String, password: String) {
        if (_uiState.value == AuthUiState.Loading) return
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            _uiState.value = repository.signup(username, email, password).toUiState(isNewAccount = true)
        }
    }

    suspend fun logout() = repository.logout()

    /** Call once a Success/Error has been shown, so it isn't replayed after a configuration change. */
    fun onStateHandled() {
        _uiState.value = AuthUiState.Idle
    }

    private fun Result<AuthResponse>.toUiState(isNewAccount: Boolean): AuthUiState = fold(
        onSuccess = { AuthUiState.Success(it.user?.profile?.username, isNewAccount) },
        onFailure = { AuthUiState.Error(it.toUserMessage(isNewAccount)) }
    )

    private fun Throwable.toUserMessage(isSignup: Boolean): String {
        val app = getApplication<Application>()
        return when (this) {
            is ApiException -> when {
                // Wrong password and unknown email both come back as 403 "Access denied"
                statusCode == 401 || statusCode == 403 ->
                    app.getString(R.string.error_invalid_credentials)
                // The backend surfaces Postgres unique-constraint violations as 500s
                serverMessage?.contains("duplicate key", ignoreCase = true) == true ->
                    app.getString(R.string.error_account_exists)
                statusCode == 400 ->
                    app.getString(if (isSignup) R.string.error_signup_rejected else R.string.error_bad_request)
                statusCode == 429 -> app.getString(R.string.error_too_many_requests)
                statusCode >= 500 -> app.getString(R.string.error_server, statusCode)
                !serverMessage.isNullOrBlank() -> serverMessage
                else -> app.getString(R.string.error_unknown_http, statusCode)
            }
            is SocketTimeoutException -> app.getString(R.string.error_timeout)
            is UnknownHostException, is ConnectException -> app.getString(R.string.error_no_connection)
            is IOException -> app.getString(R.string.error_no_connection)
            else -> app.getString(R.string.error_unexpected)
        }
    }
}
