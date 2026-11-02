package com.telesales.assistant.data.repository

import com.telesales.assistant.data.network.ApiService
import com.telesales.assistant.data.network.ApiServiceProvider
import com.telesales.assistant.data.network.CustomerPageDto
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
            }
        }
    }
}
