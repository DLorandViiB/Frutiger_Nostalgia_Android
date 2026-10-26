package com.example.firstproject.ui.habit

import androidx.annotation.StringRes
import com.example.firstproject.R

/**
 * Client-side checks mirroring CreateHabitDto: name and goal are required non-empty strings,
 * description is optional, categoryId is a required number. The columns have no length limit.
 * Values are trimmed first, so whitespace-only input counts as empty (the DTO would accept it).
 * Each function returns an error string resource, or null when the value is valid.
 */
object HabitInputValidator {

    @StringRes
    fun validateName(name: String): Int? =
        if (name.isBlank()) R.string.error_required_habit_name else null

    @StringRes
    fun validateGoal(goal: String): Int? =
        if (goal.isBlank()) R.string.error_required_goal else null

    @StringRes
    fun validateCategory(categoryId: Long?): Int? =
        if (categoryId == null) R.string.error_required_category else null
}
