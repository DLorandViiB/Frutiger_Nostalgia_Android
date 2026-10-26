package com.example.firstproject.model

import com.google.gson.annotations.SerializedName
import java.time.Instant

// Shapes mirror the NestJS backend (src/habit, src/schedule/schedule.service.ts mapToResponseDto).
// The backend mixes snake_case (start_time, is_participant_only...) and camelCase (iconUrl, scheduleId).
// Everything except primitive ids is nullable: Gson ignores Kotlin nullability, and the backend sends
// null for e.g. notes, end_time and duration_minutes.
// Timestamps are UTC ISO-8601 with "Z" and are parsed to Instant (see utils/InstantTypeAdapter).

/** GET /habit/categories item, also nested in every habit. Seeded by the backend on boot. */
data class HabitCategory(
    val id: Long,
    val name: String?,
    val iconUrl: String?
)

/** A habit as returned by /habit and nested in each schedule. */
data class HabitResponse(
    val id: Long,
    val name: String?,
    val description: String?,
    val category: HabitCategory?,
    val goal: String?,
    @SerializedName("created_at") val createdAt: Instant?,
    @SerializedName("updated_at") val updatedAt: Instant?
)

/** One concrete occurrence of a habit, e.g. an item of GET /schedule/day?date=YYYY-MM-DD. */
data class ScheduleResponse(
    val id: Long,
    @SerializedName("start_time") val startTime: Instant?,
    @SerializedName("end_time") val endTime: Instant?,
    /** "Planned", "Completed" or "Skipped"; see [ScheduleStatus]. */
    val status: String?,
    /**
     * The calendar day the schedule belongs to, sent as UTC midnight ("2026-10-26T00:00:00.000Z").
     * Kept as a string on purpose: converting it to the device zone would shift the day west of UTC.
     */
    val date: String?,
    @SerializedName("is_custom") val isCustom: Boolean?,
    @SerializedName("created_at") val createdAt: Instant?,
    @SerializedName("updated_at") val updatedAt: Instant?,
    /** "custom" or "recurring". */
    val type: String?,
    @SerializedName("duration_minutes") val durationMinutes: Int?,
    val notes: String?,
    val participants: List<ParticipantDto>?,
    val habit: HabitResponse?,
    val progress: List<ProgressResponseDto>?,
    @SerializedName("is_participant_only") val isParticipantOnly: Boolean?
) {
    /** The backend may omit end_time; fall back to start + duration. */
    val effectiveEndTime: Instant?
        get() = endTime ?: durationMinutes?.let { startTime?.plusSeconds(it * 60L) }
}

data class ParticipantDto(
    val id: Long,
    val name: String?,
    val email: String?,
    @SerializedName("profile_image") val profileImage: String?
)

data class ProgressResponseDto(
    val id: Long,
    val scheduleId: Long?,
    val date: Instant?,
    @SerializedName("logged_time") val loggedTime: Int?,
    @SerializedName("is_completed") val isCompleted: Boolean?,
    val notes: String?,
    @SerializedName("created_at") val createdAt: Instant?,
    @SerializedName("updated_at") val updatedAt: Instant?
)

/** Body of POST /habit (CreateHabitDto). name, categoryId and goal are required. */
data class CreateHabitRequest(
    val name: String,
    val description: String?,
    val categoryId: Long,
    val goal: String
)

/** The backend's RepeatPattern enum (src/schedule/dto/create-reccuring-schedule.dto.ts). */
enum class RepeatPattern {
    /** One occurrence; sent to POST /schedule/custom (the recurring endpoint would expand "none" daily). */
    @SerializedName("none") NONE,
    @SerializedName("daily") DAILY,
    @SerializedName("weekdays") WEEKDAYS,
    @SerializedName("weekends") WEEKENDS
}

/**
 * Body of POST /schedule/recurring (CreateRecurringScheduleDto). The backend expands it into one
 * row per matching day for [repeatDays] days starting from its own "today", using only the
 * time of day of [startTime].
 */
data class CreateRecurringScheduleRequest(
    val habitId: Long,
    @SerializedName("start_time") val startTime: Instant,
    @SerializedName("duration_minutes") val durationMinutes: Int,
    val repeatPattern: RepeatPattern,
    val repeatDays: Int,
    val participantIds: List<Long> = emptyList(),
    val notes: String?
)

/**
 * Body of POST /schedule/custom (CreateCustomScheduleDto): a single occurrence. The backend requires
 * [date] (YYYY-MM-DD) to be the same UTC day as [startTime].
 */
data class CreateCustomScheduleRequest(
    val habitId: Long,
    val date: String,
    @SerializedName("start_time") val startTime: Instant,
    @SerializedName("duration_minutes") val durationMinutes: Int,
    val participantIds: List<Long> = emptyList(),
    val notes: String?
)

/** Known values of [ScheduleResponse.status]. Unknown values map to null so new statuses don't crash. */
enum class ScheduleStatus(val apiValue: String) {
    PLANNED("Planned"),
    COMPLETED("Completed"),
    SKIPPED("Skipped");

    companion object {
        fun fromApi(value: String?): ScheduleStatus? =
            entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) }
    }
}
