package com.telesales.assistant.data.repository

import com.telesales.assistant.data.network.ApiService
import com.telesales.assistant.data.network.ApiServiceProvider
import com.telesales.assistant.data.network.CustomerPageDto
import com.telesales.assistant.data.network.CustomerProfileDto
import com.telesales.assistant.data.network.CustomerProfileResponseDto
import com.telesales.assistant.data.network.CustomerCreateDto
import com.telesales.assistant.data.network.CustomerUpdateDto
import com.telesales.assistant.data.network.InteractionPageDto
import com.telesales.assistant.data.network.InteractionCreateDto
import com.telesales.assistant.data.network.InteractionCreateResponseDto
import com.telesales.assistant.data.network.FollowUpPageDto
import com.telesales.assistant.data.network.FollowUpActionDto
import com.telesales.assistant.data.network.FollowUpResponseDto
import com.telesales.assistant.data.network.FollowUpDto
import com.telesales.assistant.data.network.FollowUpCustomerDto
import com.telesales.assistant.data.network.CustomerStatusDto
import com.telesales.assistant.data.network.PingDto
import com.telesales.assistant.data.settings.InMemoryPreferencesDataStore
import com.telesales.assistant.data.settings.SettingsStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CustomerRepositoryTest {
    @Test fun missingBaseUrlOrTokenDoesNotCreateApiOrMakeRequest() = runTest {
        val provider = SpyProvider()
        val repository = ApiCustomerRepository(SettingsStore(InMemoryPreferencesDataStore()), provider)

        val result = repository.getCustomers(CustomerQuery("", "active", null, null), 1)

        assertEquals(CustomerLoadResult.SetupRequired, result)
        assertEquals(0, provider.createCalls)
    }

    private class SpyProvider : ApiServiceProvider {
        var createCalls = 0
        override fun create(baseUrl: String, token: String): ApiService {
            createCalls++
            return object : ApiService {
                override suspend fun ping() = PingDto()
                override suspend fun customers(query: String?, status: String, potential: String?, lastResult: String?, page: Int, perPage: Int) = CustomerPageDto()
                override suspend fun customer(id: Long) = CustomerProfileResponseDto(CustomerProfileDto(id, phone = "0901234567"))
                override suspend fun createCustomer(customer: CustomerCreateDto) = CustomerProfileResponseDto(CustomerProfileDto(1, phone = customer.phone))
                override suspend fun updateCustomer(id: Long, customer: CustomerUpdateDto) = CustomerProfileResponseDto(CustomerProfileDto(id, phone = customer.phone))
                override suspend fun interactions(id: Long, page: Int, perPage: Int) = InteractionPageDto()
                override suspend fun createInteraction(id: Long, interaction: InteractionCreateDto) = InteractionCreateResponseDto(com.telesales.assistant.data.network.InteractionDto(1, interaction.result, interaction.needs, interaction.note, "2026-11-02T00:00:00+00:00"))
                override suspend fun followUps(status: String, group: String, page: Int, perPage: Int) = FollowUpPageDto()
                override suspend fun updateFollowUp(id: Long, request: FollowUpActionDto) = FollowUpResponseDto(FollowUpDto(id, "2026-11-02T00:00:00+00:00", "pending", "today", FollowUpCustomerDto(1, phone = "0900000000")))
                override suspend fun updateCustomerStatus(id: Long, status: CustomerStatusDto) = CustomerProfileResponseDto(CustomerProfileDto(id, phone = "0901234567", status = status.status))
            }
        }
    }
}
