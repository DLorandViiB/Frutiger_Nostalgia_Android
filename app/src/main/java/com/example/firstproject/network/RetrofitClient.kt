package com.example.firstproject.network

import android.content.Context
import com.example.firstproject.BuildConfig
import com.example.firstproject.utils.InstantTypeAdapter
import com.example.firstproject.utils.SessionManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Instant
import java.util.concurrent.TimeUnit

object RetrofitClient {

    @Volatile
    private var apiService: ApiService? = null

    fun getApiService(context: Context): ApiService =
        apiService ?: synchronized(this) {
            apiService ?: create(context.applicationContext).also { apiService = it }
        }

    private fun create(appContext: Context): ApiService {
        val sessionManager = SessionManager(appContext)
        val gson = GsonBuilder()
            .registerTypeAdapter(Instant::class.java, InstantTypeAdapter())
            .create()

        val baseClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()

        // The token refresh call uses a bare client, so it can never trigger the authenticator itself
        val refreshService = retrofit(baseClient.newBuilder().addDebugLogging().build(), gson)
            .create(AuthRefreshService::class.java)

        val client = baseClient.newBuilder()
            .addInterceptor(AuthInterceptor(sessionManager))
            .addDebugLogging()
            .authenticator(TokenAuthenticator(sessionManager, refreshService))
            .build()

        return retrofit(client, gson).create(ApiService::class.java)
    }

    private fun retrofit(client: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    private fun OkHttpClient.Builder.addDebugLogging() = apply {
        if (BuildConfig.DEBUG) {
            // HEADERS, not BODY: request bodies contain passwords and must never reach Logcat.
            addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.HEADERS
                    redactHeader("Authorization")
                }
            )
        }
    }
}
