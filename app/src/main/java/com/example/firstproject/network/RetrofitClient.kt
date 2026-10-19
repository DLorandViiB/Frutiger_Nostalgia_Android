package com.example.firstproject.network

import android.content.Context
import com.example.firstproject.BuildConfig
import com.example.firstproject.utils.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    @Volatile
    private var apiService: ApiService? = null

    fun getApiService(context: Context): ApiService =
        apiService ?: synchronized(this) {
            apiService ?: create(context.applicationContext).also { apiService = it }
        }

    private fun create(appContext: Context): ApiService {
        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(SessionManager(appContext)))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            // HEADERS, not BODY: request bodies contain passwords and must never reach Logcat.
            clientBuilder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.HEADERS
                    redactHeader("Authorization")
                }
            )
        }

        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(clientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
