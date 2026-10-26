package com.example.firstproject.utils

import android.util.Log
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/**
 * Reads the backend's ISO-8601 timestamps ("2026-10-26T05:15:00.000Z") as an absolute [Instant].
 * Keeping the offset is what lets the UI convert to the device zone; dropping the "Z" and parsing a
 * LocalDateTime would show UTC wall-clock times as if they were local.
 * An unparseable value becomes null instead of failing the whole response.
 */
class InstantTypeAdapter : TypeAdapter<Instant>() {

    override fun write(out: JsonWriter, value: Instant?) {
        if (value == null) out.nullValue() else out.value(value.toString())
    }

    override fun read(reader: JsonReader): Instant? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        val raw = reader.nextString()
        return try {
            OffsetDateTime.parse(raw).toInstant()
        } catch (e: DateTimeParseException) {
            Log.w(TAG, "Unparseable timestamp: $raw", e)
            null
        }
    }

    private companion object {
        const val TAG = "InstantTypeAdapter"
    }
}
