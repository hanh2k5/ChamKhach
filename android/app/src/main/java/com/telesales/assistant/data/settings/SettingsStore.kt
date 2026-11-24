package com.telesales.assistant.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

const val PRODUCTION_BASE_URL = "https://chamkhach.onrender.com/api/v1/"
const val PRODUCTION_TOKEN = "2005"

data class AppSettings(
    val baseUrl: String = "",
    val token: String = "",
)

class SettingsStore(
    private val dataStore: DataStore<Preferences>,
    private val defaultBaseUrl: String = "",
    private val defaultToken: String = "",
) {
    constructor(context: Context) : this(
        dataStore = context.settingsDataStore,
        defaultBaseUrl = PRODUCTION_BASE_URL,
        defaultToken = PRODUCTION_TOKEN,
    )

    val settings: Flow<AppSettings> = dataStore.data.map { preferences ->
        val url = preferences[BASE_URL]
        val t = preferences[TOKEN]
        AppSettings(
            baseUrl = if (url.isNullOrBlank()) defaultBaseUrl else url,
            token = if (t.isNullOrBlank()) defaultToken else t,
        )
    }

    suspend fun save(baseUrl: String, token: String) {
        dataStore.edit { preferences ->
            preferences[BASE_URL] = baseUrl
            preferences[TOKEN] = token
        }
    }

    private companion object {
        val BASE_URL = stringPreferencesKey("base_url")
        val TOKEN = stringPreferencesKey("token")
    }
}
