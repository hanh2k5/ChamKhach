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

data class AppSettings(
    val baseUrl: String = "",
    val token: String = "",
)

class SettingsStore(private val dataStore: DataStore<Preferences>) {
    constructor(context: Context) : this(context.settingsDataStore)

    val settings: Flow<AppSettings> = dataStore.data.map { preferences ->
        AppSettings(
            baseUrl = preferences[BASE_URL].orEmpty(),
            token = preferences[TOKEN].orEmpty(),
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
