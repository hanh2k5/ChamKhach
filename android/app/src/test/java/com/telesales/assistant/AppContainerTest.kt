package com.telesales.assistant

import com.telesales.assistant.data.network.ApiServiceFactory
import com.telesales.assistant.data.settings.SettingsStore
import com.telesales.assistant.data.settings.InMemoryPreferencesDataStore
import com.telesales.assistant.di.AppContainer
import com.telesales.assistant.data.repository.ApiCustomerRepository
import com.telesales.assistant.data.repository.CustomerFormRepository
import com.telesales.assistant.data.repository.CustomerDetailRepository
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppContainerTest {
    @Test
    fun appContainer_exposesSettingsAndApiDependencies() {
        val container = object : AppContainer {
            override val settingsStore = SettingsStore(InMemoryPreferencesDataStore())
            override val apiServiceFactory = ApiServiceFactory()
            private val customerApiRepository = ApiCustomerRepository(settingsStore, apiServiceFactory)
            override val customerRepository = customerApiRepository
            override val customerFormRepository: CustomerFormRepository = customerApiRepository
            override val customerDetailRepository: CustomerDetailRepository = customerApiRepository
        }

        assertNotNull(container.settingsStore)
        assertNotNull(container.apiServiceFactory)
        assertTrue(container is AppContainer)
    }
}

