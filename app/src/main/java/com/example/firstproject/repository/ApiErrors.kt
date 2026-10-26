package com.example.firstproject.repository

import com.example.firstproject.model.ErrorResponse
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import retrofit2.Response

/** Non-2xx answer from the backend, with the message parsed from its error body (if any). */
class ApiException(val statusCode: Int, val serverMessage: String?) :
    Exception("HTTP $statusCode${serverMessage?.let { ": $it" } ?: ""}")

private val errorGson = Gson()

/** Builds an [ApiException] for a failed response. */
fun Response<*>.toApiException() = ApiException(code(), parseErrorMessage())

/** The body of a successful response, or an [ApiException] for an error / empty body. */
fun <T> Response<T>.bodyOrThrow(): T {
    val body = body()
    if (isSuccessful && body != null) return body
    if (isSuccessful) throw IllegalStateException("Unexpected empty response from server")
    throw toApiException()
}

/** runCatching that lets coroutine cancellation propagate instead of turning it into a failure. */
inline fun <T> runApiCatching(block: () -> T): Result<T> =
    runCatching(block).onFailure { if (it is CancellationException) throw it }

/** Reads {"statusCode", "message"}; message may be a string or an array of strings. */
private fun Response<*>.parseErrorMessage(): String? = runCatching {
    val raw = errorBody()?.string().orEmpty()
    val message = errorGson.fromJson(raw, ErrorResponse::class.java)?.message ?: return@runCatching null
    when {
        message.isJsonArray -> message.asJsonArray.joinToString("\n") { it.asString }
        message.isJsonPrimitive -> message.asString
        else -> null
    }
}.getOrNull()?.takeIf { it.isNotBlank() }
