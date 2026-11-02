package com.telesales.assistant.data.settings

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsStoreTest {
    @Test
    fun savesAndReadsBaseUrlAndToken() = runTest {
        val store = SettingsStore(InMemoryPreferencesDataStore())

        assertEquals(AppSettings(), store.settings.first())
        store.save("http://10.0.2.2:8000/api/v1/", "sample-token")

        assertEquals(
            AppSettings("http://10.0.2.2:8000/api/v1/", "sample-token"),
            store.settings.first(),
        )
    }
}
