package com.example.firstproject.repository

import com.example.firstproject.model.CreateCustomScheduleRequest
import com.example.firstproject.model.CreateRecurringScheduleRequest
import com.example.firstproject.model.RepeatPattern
import com.example.firstproject.model.ScheduleResponse
import com.example.firstproject.network.ApiService
import java.time.Instant
import java.time.LocalDate

class ScheduleRepository(private val api: ApiService) {

    /** Schedules on [day] (a local calendar date), sorted by start time; ones without a start time go last. */
    suspend fun getSchedulesForDay(day: LocalDate): Result<List<ScheduleResponse>> = runApiCatching {
        // LocalDate.toString() is ISO-8601 (YYYY-MM-DD), the format the backend expects
        api.getSchedulesByDay(day.toString()).bodyOrThrow().sortedWith(
            compareBy(nullsLast()) { it: ScheduleResponse -> it.startTime }
                .thenBy { it.habit?.name.orEmpty().lowercase() }
        )
    }

    /**
     * Creates the schedule and returns the created occurrences. [RepeatPattern.NONE] is a single
     * occurrence on [date]; repeating patterns start from today and ignore [date] (backend behaviour).
     */
    suspend fun createSchedule(
        habitId: Long,
        date: LocalDate,
        start: Instant,
        durationMinutes: Int,
        pattern: RepeatPattern,
        repeatDays: Int,
        notes: String?
    ): Result<List<ScheduleResponse>> = runApiCatching {
        if (pattern == RepeatPattern.NONE) {
            listOf(
                api.createCustomSchedule(
                    CreateCustomScheduleRequest(
                        habitId = habitId,
                        date = date.toString(),
                        startTime = start,
                        durationMinutes = durationMinutes,
                        notes = notes
                    )
                ).bodyOrThrow()
            )
        } else {
            api.createRecurringSchedule(
                CreateRecurringScheduleRequest(
                    habitId = habitId,
                    startTime = start,
                    durationMinutes = durationMinutes,
                    repeatPattern = pattern,
                    repeatDays = repeatDays,
                    notes = notes
                )
            ).bodyOrThrow()
        }
    }
}
