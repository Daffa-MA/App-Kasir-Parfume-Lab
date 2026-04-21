package com.example.appkasir.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // GANTI DENGAN URL DEPLOYMENT KAMU (Railway/Render/Heroku)
    // Contoh: "https://your-app.up.railway.app/api/"
    private const val BASE_URL = "https://your-railway-app.up.railway.app/api/"
    private const val API_TOKEN = "dev-pos-token"

    private val logger = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            if (API_TOKEN.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $API_TOKEN")
            }
            chain.proceed(requestBuilder.build())
        }
        .addInterceptor(logger)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
