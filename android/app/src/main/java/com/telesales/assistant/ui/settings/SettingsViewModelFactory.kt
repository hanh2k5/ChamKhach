package com.telesales.assistant.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.telesales.assistant.data.network.ApiServiceProvider
import com.telesales.assistant.data.settings.SettingsStore

class SettingsViewModelFactory(
    private val settingsStore: SettingsStore,
    private val apiServiceFactory: ApiServiceProvider,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(SettingsViewModel::class.java))
        return modelClass.cast(SettingsViewModel(settingsStore, apiServiceFactory))
    }
}
