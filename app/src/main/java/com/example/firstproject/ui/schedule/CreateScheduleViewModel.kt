package com.example.firstproject.ui.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.firstproject.R
import com.example.firstproject.model.HabitResponse
import com.example.firstproject.model.RepeatPattern
import com.example.firstproject.network.RetrofitClient
import com.example.firstproject.repository.HabitRepository
import com.example.firstproject.repository.ScheduleRepository
import com.example.firstproject.ui.common.isSessionExpired
import com.example.firstproject.ui.common.toUserMessage
import com.example.firstproject.utils.SessionManager
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

sealed class HabitsState {
    object Loading : HabitsState()
    data class Loaded(val habits: List<HabitResponse>) : HabitsState()
    data class Error(val message: String) : HabitsState()
}

sealed class CreateScheduleUiState {
    object Idle : CreateScheduleUiState()
    object Loading : CreateScheduleUiState()
    data class Success(val habitName: String?, val sessions: Int) : CreateScheduleUiState()
    data class Error(val message: String) : CreateScheduleUiState()
    object SessionExpired : CreateScheduleUiState()
}

/**
 * Form selections that aren't plain text fields (habit, date, time, pattern) live in [SavedStateHandle],
 * so they survive rotation and process death. The nav argument "habitId" arrives through it too.
 */
class CreateScheduleViewModel(
    application: Application,
    private val savedState: SavedStateHandle
) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val api = RetrofitClient.getApiService(application)
    private val habitRepository = HabitRepository(api)
    private val scheduleRepository = ScheduleRepository(api)

    private val _habits = MutableLiveData<HabitsState>(HabitsState.Loading)
    val habits: LiveData<HabitsState> = _habits

    val selectedHabitId: LiveData<Long?> =
        savedState.getLiveData(CreateScheduleFragment.ARG_HABIT_ID, CreateScheduleFragment.NO_HABIT)
            .map { id -> id.takeIf { it != CreateScheduleFragment.NO_HABIT } }

    val date: LiveData<LocalDate> =
        savedState.getLiveData(KEY_DATE, LocalDate.now().toEpochDay()).map(LocalDate::ofEpochDay)

    val startTime: LiveData<LocalTime> =
        savedState.getLiveData(KEY_START_MINUTE, defaultStartMinute()).map { LocalTime.of(it / 60, it % 60) }

    val pattern: LiveData<RepeatPattern> =
        savedState.getLiveData(KEY_PATTERN, RepeatPattern.DAILY.name).map(RepeatPattern::valueOf)

    private val _uiState = MutableLiveData<CreateScheduleUiState>(CreateScheduleUiState.Idle)
    val uiState: LiveData<CreateScheduleUiState> = _uiState

    init {
        loadHabits()
    }

    fun loadHabits() {
        _habits.value = HabitsState.Loading
        viewModelScope.launch {
            habitRepository.getHabits().fold(
                onSuccess = { _habits.value = HabitsState.Loaded(it) },
                onFailure = {
                    if (it.isSessionExpired) expireSession()
                    else _habits.value = HabitsState.Error(it.toUserMessage(getApplication()))
                }
            )
        }
    }

    fun selectHabit(habitId: Long) {
        savedState[CreateScheduleFragment.ARG_HABIT_ID] = habitId
    }

    fun selectDate(date: LocalDate) {
        savedState[KEY_DATE] = date.toEpochDay()
    }

    fun selectStartTime(time: LocalTime) {
        savedState[KEY_START_MINUTE] = time.hour * 60 + time.minute
    }

    fun selectPattern(pattern: RepeatPattern) {
        savedState[KEY_PATTERN] = pattern.name
    }

    /** The day the (first) occurrence lands on: repeating schedules always start today on the backend. */
    fun effectiveDate(): LocalDate =
        if (pattern.value == RepeatPattern.NONE) date.value ?: LocalDate.now() else LocalDate.now()

    /** Inputs must already be validated with [ScheduleInputValidator]. */
    fun createSchedule(durationMinutes: Int, repeatDays: Int, notes: String) {
        val habitId = selectedHabitId.value ?: return
        val pattern = pattern.value ?: return
        val time = startTime.value ?: return
        if (_uiState.value == CreateScheduleUiState.Loading) return

        val day = effectiveDate()
        // Local wall-clock time -> absolute instant; Gson sends it as UTC ISO-8601 ("...Z")
        val start = day.atTime(time).atZone(ZoneId.systemDefault()).toInstant()
        val habitName = (habits.value as? HabitsState.Loaded)?.habits?.firstOrNull { it.id == habitId }?.name

        _uiState.value = CreateScheduleUiState.Loading
        viewModelScope.launch {
            scheduleRepository.createSchedule(
                habitId = habitId,
                date = day,
                start = start,
                durationMinutes = durationMinutes,
                pattern = pattern,
                repeatDays = repeatDays,
                notes = notes.trim().ifEmpty { null }
            ).fold(
                onSuccess = { created ->
                    _uiState.value = if (created.isEmpty()) {
                        // e.g. "Weekends" with 1 day ahead on a Monday: the backend creates nothing
                        CreateScheduleUiState.Error(
                            getApplication<Application>().getString(R.string.error_schedule_no_matches)
                        )
                    } else {
                        CreateScheduleUiState.Success(habitName, created.size)
                    }
                },
                onFailure = {
                    if (it.isSessionExpired) expireSession()
                    else _uiState.value = CreateScheduleUiState.Error(it.toUserMessage(getApplication()))
                }
            )
        }
    }

    /** Call once a Success/Error has been shown, so it isn't replayed after a configuration change. */
    fun onStateHandled() {
        _uiState.value = CreateScheduleUiState.Idle
    }

    private fun expireSession() {
        sessionManager.clearSession()
        _uiState.value = CreateScheduleUiState.SessionExpired
    }

    /** The next quarter hour, kept on today. */
    private fun defaultStartMinute(): Int {
        val now = LocalTime.now()
        val next = (now.hour * 60 + now.minute + 14) / 15 * 15
        return next.coerceAtMost(23 * 60 + 45)
    }

    private companion object {
        const val KEY_DATE = "date_epoch_day"
        const val KEY_START_MINUTE = "start_minute_of_day"
        const val KEY_PATTERN = "repeat_pattern"
    }
}
