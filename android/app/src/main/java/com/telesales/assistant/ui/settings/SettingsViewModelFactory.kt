package com.telesales.assistant.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.telesales.assistant.data.network.ApiServiceProvider
import com.telesales.assistant.data.settings.SettingsStore

class SettingsViewModelFactory(
    private val settingsStore: SettingsStore,
    private val apiServiceFactory: ApiServiceProvider,
    private val permissionStateProvider: ReminderPermissionStateProvider? = null,
    private val reminderCoordinator: com.telesales.assistant.reminder.ReminderCoordinator? = null,
    private val debugBuild: Boolean = false,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(SettingsViewModel::class.java))
        return modelClass.cast(SettingsViewModel(settingsStore, apiServiceFactory, permissionStateProvider, reminderCoordinator, debugBuild = debugBuild))
    }
}
