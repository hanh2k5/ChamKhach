package com.telesales.assistant.data.network

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

class ApiServiceFactory : ApiServiceProvider {
    override fun create(baseUrl: String, token: String): ApiService {
        val normalizedUrl = requireNotNull(BaseUrlNormalizer.normalizeOrNull(baseUrl)) {
            "Base URL is invalid"
        }
        val contentType = "application/json".toMediaType()
        val json = Json { ignoreUnknownKeys = true }
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor { token })
            .build()

        return Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(ApiService::class.java)
    }
}
