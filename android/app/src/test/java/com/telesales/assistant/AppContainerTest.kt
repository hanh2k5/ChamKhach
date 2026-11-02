package com.telesales.assistant

import com.telesales.assistant.data.network.ApiServiceFactory
import com.telesales.assistant.data.settings.SettingsStore
import com.telesales.assistant.data.settings.InMemoryPreferencesDataStore
import com.telesales.assistant.di.AppContainer
import com.telesales.assistant.data.repository.ApiCustomerRepository
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppContainerTest {
    @Test
    fun appContainer_exposesSettingsAndApiDependencies() {
        val container = object : AppContainer {
            override val settingsStore = SettingsStore(InMemoryPreferencesDataStore())
            override val apiServiceFactory = ApiServiceFactory()
            override val customerRepository = ApiCustomerRepository(settingsStore, apiServiceFactory)
        }

        assertNotNull(container.settingsStore)
        assertNotNull(container.apiServiceFactory)
        assertTrue(container is AppContainer)
    }
}

