package com.example.firstproject.ui.habit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.firstproject.model.HabitCategory
import com.example.firstproject.network.RetrofitClient
import com.example.firstproject.repository.HabitRepository
import com.example.firstproject.ui.common.isSessionExpired
import com.example.firstproject.ui.common.toUserMessage
import com.example.firstproject.utils.SessionManager
import kotlinx.coroutines.launch

sealed class CategoriesState {
    object Loading : CategoriesState()
    data class Loaded(val categories: List<HabitCategory>) : CategoriesState()
    data class Error(val message: String) : CategoriesState()
}

sealed class AddHabitUiState {
    object Idle : AddHabitUiState()
    object Loading : AddHabitUiState()
    data class Success(val habitId: Long) : AddHabitUiState()
    data class Error(val message: String) : AddHabitUiState()
    object SessionExpired : AddHabitUiState()
}

class AddHabitViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val repository = HabitRepository(RetrofitClient.getApiService(application))

    private val _categories = MutableLiveData<CategoriesState>(CategoriesState.Loading)
    val categories: LiveData<CategoriesState> = _categories

    private val _selectedCategoryId = MutableLiveData<Long?>(null)
    val selectedCategoryId: LiveData<Long?> = _selectedCategoryId

    private val _uiState = MutableLiveData<AddHabitUiState>(AddHabitUiState.Idle)
    val uiState: LiveData<AddHabitUiState> = _uiState

    init {
        loadCategories()
    }

    fun loadCategories() {
        _categories.value = CategoriesState.Loading
        viewModelScope.launch {
            repository.getCategories().fold(
                onSuccess = { _categories.value = CategoriesState.Loaded(it) },
                onFailure = {
                    if (it.isSessionExpired) expireSession()
                    else _categories.value = CategoriesState.Error(it.toUserMessage(getApplication()))
                }
            )
        }
    }

    fun selectCategory(categoryId: Long) {
        _selectedCategoryId.value = categoryId
    }

    /** Inputs must already be validated with [HabitInputValidator]. */
    fun createHabit(name: String, description: String, goal: String) {
        val categoryId = _selectedCategoryId.value ?: return
        if (_uiState.value == AddHabitUiState.Loading) return
        _uiState.value = AddHabitUiState.Loading
        viewModelScope.launch {
            repository.createHabit(
                name = name.trim(),
                description = description.trim().ifEmpty { null },
                categoryId = categoryId,
                goal = goal.trim()
            ).fold(
                onSuccess = { _uiState.value = AddHabitUiState.Success(it.id) },
                onFailure = {
                    if (it.isSessionExpired) expireSession()
                    else _uiState.value = AddHabitUiState.Error(it.toUserMessage(getApplication()))
                }
            )
        }
    }

    /** Call once a Success/Error has been shown, so it isn't replayed after a configuration change. */
    fun onStateHandled() {
        _uiState.value = AddHabitUiState.Idle
    }

    private fun expireSession() {
        sessionManager.clearSession()
        _uiState.value = AddHabitUiState.SessionExpired
    }
}
