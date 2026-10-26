package com.example.firstproject.repository

import com.example.firstproject.model.CreateHabitRequest
import com.example.firstproject.model.HabitCategory
import com.example.firstproject.model.HabitResponse
import com.example.firstproject.network.ApiService

class HabitRepository(private val api: ApiService) {

    /** Categories in the backend's order (by id). */
    suspend fun getCategories(): Result<List<HabitCategory>> = runApiCatching {
        api.getHabitCategories().bodyOrThrow().sortedBy { it.id }
    }

    /** The user's habits, alphabetically. */
    suspend fun getHabits(): Result<List<HabitResponse>> = runApiCatching {
        api.getHabits().bodyOrThrow().sortedBy { it.name.orEmpty().lowercase() }
    }

    suspend fun createHabit(
        name: String,
        description: String?,
        categoryId: Long,
        goal: String
    ): Result<HabitResponse> = runApiCatching {
        api.createHabit(CreateHabitRequest(name, description, categoryId, goal)).bodyOrThrow()
    }
}
