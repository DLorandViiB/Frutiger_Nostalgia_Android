package com.example.firstproject.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.firstproject.model.ScheduleResponse
import com.example.firstproject.network.RetrofitClient
import com.example.firstproject.repository.ScheduleRepository
import com.example.firstproject.ui.common.isSessionExpired
import com.example.firstproject.ui.common.toUserMessage
import com.example.firstproject.utils.SessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed class HomeUiState {
    object Loading : HomeUiState()
    /** An empty list is the "nothing scheduled" state. */
    data class Success(val schedules: List<ScheduleResponse>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
    /** The access token was rejected and could not be refreshed; the session has been cleared. */
    object SessionExpired : HomeUiState()
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val repository = ScheduleRepository(RetrofitClient.getApiService(application))

    private val _uiState = MutableLiveData<HomeUiState>(HomeUiState.Loading)
    val uiState: LiveData<HomeUiState> = _uiState

    /** True while a pull-to-refresh is running; the current content stays on screen meanwhile. */
    private val _isRefreshing = MutableLiveData(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    /** The device-local day being shown. */
    private val _day = MutableLiveData(LocalDate.now())
    val day: LiveData<LocalDate> = _day

    private var loadJob: Job? = null

    init {
        load(userRefresh = false)
    }

    /** Pull-to-refresh. */
    fun refresh() = load(userRefresh = true)

    /** Retry button: shows the full loading state again. */
    fun retry() = load(userRefresh = false)

    /** Reloads when the app comes back after midnight, so "today" stays today. */
    fun reloadIfDayChanged() {
        if (_day.value != LocalDate.now()) load(userRefresh = false)
    }

    private fun load(userRefresh: Boolean) {
        if (loadJob?.isActive == true) {
            // Let the running request finish; it clears the refresh spinner when done
            if (userRefresh) _isRefreshing.value = true
            return
        }
        val today = LocalDate.now()
        _day.value = today
        if (userRefresh) _isRefreshing.value = true else _uiState.value = HomeUiState.Loading

        loadJob = viewModelScope.launch {
            val result = repository.getSchedulesForDay(today)
            _uiState.value = result.fold(
                onSuccess = { HomeUiState.Success(it) },
                onFailure = { it.toUiState() }
            )
            _isRefreshing.value = false
        }
    }

    private fun Throwable.toUiState(): HomeUiState {
        if (isSessionExpired) {
            // TokenAuthenticator already tried to refresh; make sure nothing stale is left behind
            sessionManager.clearSession()
            return HomeUiState.SessionExpired
        }
        return HomeUiState.Error(toUserMessage(getApplication()))
    }
}
