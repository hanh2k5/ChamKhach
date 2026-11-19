package com.telesales.assistant.data.network

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

class ApiServiceFactory : ApiServiceProvider {
    private val client = OkHttpClient.Builder()
        .connectionPool(okhttp3.ConnectionPool(5, 5, java.util.concurrent.TimeUnit.MINUTES))
        .build()
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private val contentType = "application/json".toMediaType()

    @Volatile
    private var cachedToken: String = ""
    @Volatile
    private var cachedUrl: String = ""
    @Volatile
    private var cachedService: ApiService? = null

    override fun create(baseUrl: String, token: String): ApiService {
        val normalizedUrl = requireNotNull(BaseUrlNormalizer.normalizeOrNull(baseUrl)) {
            "Base URL is invalid"
        }
        val existing = cachedService
        if (existing != null && cachedUrl == normalizedUrl && cachedToken == token) {
            return existing
        }

        val authClient = client.newBuilder()
            .addInterceptor(AuthInterceptor { token })
            .build()

        val service = Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(authClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(ApiService::class.java)

        cachedUrl = normalizedUrl
        cachedToken = token
        cachedService = service
        return service
    }
}
