package com.telesales.assistant.di

import android.content.Context
import com.telesales.assistant.data.network.ApiServiceFactory
import com.telesales.assistant.data.network.ApiServiceProvider
import com.telesales.assistant.data.settings.SettingsStore

/**
 * Interface cho Dependency Container theo mô hình Manual DI (SRS mục 2 & 9.1).
 * Các dependency cụ thể (SettingsRepository, CustomerRepository, Database, ApiService)
 * sẽ được cung cấp qua container này ở các task A1..A8.
 */
interface AppContainer {
    val settingsStore: SettingsStore
    val apiServiceFactory: ApiServiceProvider
}

class DefaultAppContainer(context: Context) : AppContainer {
    override val settingsStore = SettingsStore(context)
    override val apiServiceFactory = ApiServiceFactory()
}

