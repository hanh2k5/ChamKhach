package com.telesales.assistant.data.network

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("ping")
    suspend fun ping(): PingDto

    @GET("customers")
    suspend fun customers(
        @Query("q") query: String?,
        @Query("status") status: String,
        @Query("potential") potential: String?,
        @Query("last_result") lastResult: String?,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
    ): CustomerPageDto
}

@Serializable data class CustomerPageDto(val data: List<CustomerDto> = emptyList(), val links: CustomerPageLinksDto = CustomerPageLinksDto(), val meta: CustomerPageMetaDto = CustomerPageMetaDto())
@Serializable data class CustomerPageLinksDto(val next: String? = null)
@Serializable data class CustomerPageMetaDto(val current_page: Int = 1, val last_page: Int = 1, val per_page: Int = 20, val total: Int = 0)
@Serializable data class CustomerDto(
    val id: Long,
    val company_name: String? = null,
    val contact_name: String? = null,
    val phone: String,
    val industry: String? = null,
    val potential_level: String = "unrated",
    val status: String = "active",
    val interaction_count: Int = 0,
    val last_contacted_at: String? = null,
    val last_result: String? = null,
)

@Serializable
data class PingDto(
    val status: String = "",
    val message: String = "",
)
