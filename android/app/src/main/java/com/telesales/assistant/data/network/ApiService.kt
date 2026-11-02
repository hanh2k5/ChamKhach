package com.telesales.assistant.data.network

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Body
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

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

    @GET("customers/{id}")
    suspend fun customer(@Path("id") id: Long): CustomerProfileResponseDto

    @POST("customers")
    suspend fun createCustomer(@Body customer: CustomerCreateDto): CustomerProfileResponseDto

    @PATCH("customers/{id}")
    suspend fun updateCustomer(@Path("id") id: Long, @Body customer: CustomerUpdateDto): CustomerProfileResponseDto

    @GET("customers/{id}/interactions")
    suspend fun interactions(
        @Path("id") id: Long,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
    ): InteractionPageDto

    @PATCH("customers/{id}")
    suspend fun updateCustomerStatus(@Path("id") id: Long, @Body status: CustomerStatusDto): CustomerProfileResponseDto
}

@Serializable data class CustomerPageDto(val data: List<CustomerDto> = emptyList(), val links: CustomerPageLinksDto = CustomerPageLinksDto(), val meta: CustomerPageMetaDto = CustomerPageMetaDto())
@Serializable data class CustomerPageLinksDto(val next: String? = null)
@Serializable data class CustomerPageMetaDto(val current_page: Int = 1, val last_page: Int = 1, val per_page: Int = 20, val total: Int = 0)
@Serializable data class CustomerProfileResponseDto(val data: CustomerProfileDto)
@Serializable data class NextFollowUpDto(val id: Long, val remind_at: String)
@Serializable data class CustomerProfileDto(
    val id: Long,
    val company_name: String? = null,
    val contact_name: String? = null,
    val phone: String,
    val address: String? = null,
    val industry: String? = null,
    val source: String? = null,
    val potential_level: String = "unrated",
    val status: String = "active",
    val interaction_count: Int = 0,
    val last_contacted_at: String? = null,
    val last_result: String? = null,
    val next_follow_up: NextFollowUpDto? = null,
    val cancelled_follow_up_ids: List<Long> = emptyList(),
)
@Serializable data class CustomerStatusDto(val status: String)
@Serializable data class InteractionPageDto(
    val data: List<InteractionDto> = emptyList(),
    val links: CustomerPageLinksDto = CustomerPageLinksDto(),
    val meta: CustomerPageMetaDto = CustomerPageMetaDto(),
)
@Serializable data class InteractionDto(
    val id: Long,
    val result: String,
    val needs: List<String> = emptyList(),
    val note: String? = null,
    val contacted_at: String,
)
@Serializable data class CustomerCreateDto(
    val company_name: String?,
    val contact_name: String?,
    val phone: String,
    val address: String?,
    val industry: String?,
    val source: String?,
    val potential_level: String,
)
@Serializable data class CustomerUpdateDto(
    val company_name: String?,
    val contact_name: String?,
    val phone: String,
    val address: String?,
    val industry: String?,
    val source: String?,
    val potential_level: String,
    val status: String,
)
@Serializable data class ApiErrorDto(
    val message: String = "",
    val code: String? = null,
    val existing_customer_id: Long? = null,
    val errors: Map<String, List<String>> = emptyMap(),
)
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
