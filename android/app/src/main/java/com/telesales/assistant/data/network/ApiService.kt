package com.telesales.assistant.data.network

import kotlinx.serialization.Serializable
import retrofit2.http.GET

interface ApiService {
    @GET("ping")
    suspend fun ping(): PingDto
}

@Serializable
data class PingDto(
    val status: String = "",
    val message: String = "",
)
