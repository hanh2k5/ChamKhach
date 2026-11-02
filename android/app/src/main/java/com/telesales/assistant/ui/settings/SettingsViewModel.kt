package com.telesales.assistant.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telesales.assistant.data.network.ApiServiceProvider
import com.telesales.assistant.data.network.BaseUrlNormalizer
import com.telesales.assistant.data.settings.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import com.telesales.assistant.reminder.ReminderAction
import com.telesales.assistant.reminder.ReminderCoordinator

enum class SettingsStatus {
    None,
    Saved,
    Checking,
    Connected,
    InvalidUrl,
    EndpointNotFound,
    MissingToken,
    Unauthorized,
    NetworkError,
    ServerError,
    StorageError,
}

data class SettingsUiState(
    val baseUrl: String = "",
    val token: String = "",
    val isLoaded: Boolean = false,
    val isChecking: Boolean = false,
    val baseUrlInvalid: Boolean = false,
    val status: SettingsStatus = SettingsStatus.None,
    val notificationsGranted: Boolean = true,
    val exactAlarmsGranted: Boolean = true,
)

class SettingsViewModel(
    private val settingsStore: SettingsStore,
    private val apiServiceFactory: ApiServiceProvider,
    private val permissionStateProvider: ReminderPermissionStateProvider? = null,
    private val reminderCoordinator: ReminderCoordinator? = null,
    private val debugBuild: Boolean = false,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = mutableState.asStateFlow()

    init { refreshReminderPermissions() }

    fun refreshReminderPermissions() {
        permissionStateProvider?.current()?.let { permissions ->
            mutableState.value = mutableState.value.copy(
                notificationsGranted = permissions.notificationsGranted,
                exactAlarmsGranted = permissions.exactAlarmsGranted,
            )
        }
    }

    fun scheduleDebugReminder() {
        if (!debugBuild) return
        viewModelScope.launch {
            val reminder = reminderCoordinator?.createDebugReminder(DEBUG_REMINDER_ID) ?: return@launch
            reminderCoordinator?.apply(listOf(ReminderAction.Schedule(reminder)))
        }
    }

    init {
        viewModelScope.launch {
            settingsStore.settings.collect { settings ->
                mutableState.value = mutableState.value.copy(
                    baseUrl = settings.baseUrl,
                    token = settings.token,
                    isLoaded = true,
                    baseUrlInvalid = settings.baseUrl.isNotBlank() &&
                        BaseUrlNormalizer.normalizeOrNull(settings.baseUrl) == null,
                )
            }
        }
    }

    fun onBaseUrlChanged(value: String) {
        mutableState.value = mutableState.value.copy(
            baseUrl = value,
            baseUrlInvalid = value.isNotBlank() && BaseUrlNormalizer.normalizeOrNull(value) == null,
            status = SettingsStatus.None,
        )
    }

    fun onTokenChanged(value: String) {
        mutableState.value = mutableState.value.copy(token = value, status = SettingsStatus.None)
    }

    fun save() {
        val current = mutableState.value
        val normalizedUrl = BaseUrlNormalizer.normalizeOrNull(current.baseUrl)
        if (normalizedUrl == null) {
            mutableState.value = current.copy(baseUrlInvalid = true, status = SettingsStatus.InvalidUrl)
            return
        }

        val token = current.token.trim()
        mutableState.value = current.copy(baseUrl = normalizedUrl, token = token, baseUrlInvalid = false)
        viewModelScope.launch {
            try {
                settingsStore.save(normalizedUrl, token)
                mutableState.value = mutableState.value.copy(status = SettingsStatus.Saved)
            } catch (_: IOException) {
                mutableState.value = mutableState.value.copy(status = SettingsStatus.StorageError)
            }
        }
    }

    fun checkConnection() {
        val current = mutableState.value
        val normalizedUrl = BaseUrlNormalizer.normalizeOrNull(current.baseUrl)
        if (normalizedUrl == null) {
            mutableState.value = current.copy(baseUrlInvalid = true, status = SettingsStatus.InvalidUrl)
            return
        }
        val token = current.token.trim()
        if (token.isBlank()) {
            mutableState.value = current.copy(baseUrl = normalizedUrl, status = SettingsStatus.MissingToken)
            return
        }

        mutableState.value = current.copy(
            baseUrl = normalizedUrl,
            token = token,
            baseUrlInvalid = false,
            isChecking = true,
            status = SettingsStatus.Checking,
        )
        viewModelScope.launch {
            try {
                settingsStore.save(normalizedUrl, token)
            } catch (_: IOException) {
                mutableState.value = mutableState.value.copy(isChecking = false, status = SettingsStatus.StorageError)
                return@launch
            }

            try {
                apiServiceFactory.create(normalizedUrl, token).ping()
                mutableState.value = mutableState.value.copy(isChecking = false, status = SettingsStatus.Connected)
            } catch (exception: HttpException) {
                val status = when {
                    exception.code() == 401 -> SettingsStatus.Unauthorized
                    exception.code() == 404 -> SettingsStatus.EndpointNotFound
                    else -> SettingsStatus.ServerError
                }
                mutableState.value = mutableState.value.copy(isChecking = false, status = status)
            } catch (_: IOException) {
                mutableState.value = mutableState.value.copy(isChecking = false, status = SettingsStatus.NetworkError)
            } catch (_: IllegalArgumentException) {
                mutableState.value = mutableState.value.copy(isChecking = false, status = SettingsStatus.InvalidUrl)
            }
        }
    }

    companion object { const val DEBUG_REMINDER_ID = Long.MIN_VALUE + 8 }
}
