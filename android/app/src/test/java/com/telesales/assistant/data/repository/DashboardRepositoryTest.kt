package com.telesales.assistant.data.repository

import com.telesales.assistant.data.network.ApiService
import com.telesales.assistant.data.network.ApiServiceProvider
import com.telesales.assistant.data.settings.InMemoryPreferencesDataStore
import com.telesales.assistant.data.settings.SettingsStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardRepositoryTest {
    @Test fun doesNotCreateApiWhenBaseUrlAndTokenAreMissing() = runTest {
        val provider = CountingProvider()
        val repository = ApiDashboardRepository(SettingsStore(InMemoryPreferencesDataStore()), provider)

        assertEquals(DashboardResult.SetupRequired, repository.getDashboard())
        assertEquals(0, provider.createCalls)
    }

    private class CountingProvider : ApiServiceProvider {
        var createCalls = 0
        override fun create(baseUrl: String, token: String): ApiService {
            createCalls++
            error("API must not be created without settings")
        }
    }
}
