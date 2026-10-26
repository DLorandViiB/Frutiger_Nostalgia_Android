package com.example.firstproject.ui.schedule

import androidx.annotation.StringRes
import com.example.firstproject.R
import com.example.firstproject.model.RepeatPattern
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Client-side checks mirroring CreateRecurringScheduleDto / CreateCustomScheduleDto and what
 * ScheduleService does with them:
 * - duration_minutes is an int and must be > 0 (0 fails with "Could not calculate duration_minutes");
 * - repeatDays is a number of days ahead (no backend limit; capped here so one tap can't create
 *   hundreds of rows and notification jobs);
 * - schedules are filed under the UTC day of their start, so the start must fall on the same UTC
 *   day as the chosen local date or the schedule would show up on a neighbouring day.
 */
object ScheduleInputValidator {

    const val DURATION_MIN = 1
    const val DURATION_MAX = 24 * 60
    const val REPEAT_DAYS_MIN = 1
    const val REPEAT_DAYS_MAX = 365

    @StringRes
    fun validateDuration(minutes: Int?): Int? =
        if (minutes == null || minutes !in DURATION_MIN..DURATION_MAX) R.string.error_duration_range else null

    @StringRes
    fun validateRepeatDays(pattern: RepeatPattern, days: Int?): Int? = when {
        pattern == RepeatPattern.NONE -> null
        days == null || days !in REPEAT_DAYS_MIN..REPEAT_DAYS_MAX -> R.string.error_repeat_days_range
        else -> null
    }

    @StringRes
    fun validateDate(date: LocalDate, today: LocalDate): Int? =
        if (date.isBefore(today)) R.string.error_date_past else null

    /** The local start times on [date] whose UTC day is still [date], or null if [time] is inside them. */
    fun utcSafeWindowIfOutside(date: LocalDate, time: LocalTime, zone: ZoneId): ClosedRange<LocalTime>? {
        val window = utcSafeWindow(date, zone)
        return if (time in window) null else window
    }

    private fun utcSafeWindow(date: LocalDate, zone: ZoneId): ClosedRange<LocalTime> {
        val utcDayStart = date.atStartOfDay(ZoneOffset.UTC).withZoneSameInstant(zone)
        val utcDayEnd = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).minusMinutes(1).withZoneSameInstant(zone)
        val from = if (utcDayStart.toLocalDate() == date) utcDayStart.toLocalTime() else LocalTime.MIDNIGHT
        val to = if (utcDayEnd.toLocalDate() == date) utcDayEnd.toLocalTime() else LocalTime.of(23, 59)
        return from..to
    }
}
