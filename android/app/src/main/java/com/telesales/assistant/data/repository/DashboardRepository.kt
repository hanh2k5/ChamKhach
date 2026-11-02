package com.telesales.assistant.data.repository

import com.telesales.assistant.data.mapper.toDomain
import com.telesales.assistant.data.network.ApiServiceProvider
import com.telesales.assistant.data.settings.SettingsStore
import com.telesales.assistant.domain.model.DashboardSummary
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException

sealed interface DashboardResult {
    data class Success(val dashboard: DashboardSummary) : DashboardResult
    data object SetupRequired : DashboardResult
    data object NetworkError : DashboardResult
    data object Unauthorized : DashboardResult
    data object ServerError : DashboardResult
}

interface DashboardRepository {
    suspend fun getDashboard(): DashboardResult
}

class ApiDashboardRepository(
    private val settingsStore: SettingsStore,
    private val apiServiceProvider: ApiServiceProvider,
) : DashboardRepository {
    override suspend fun getDashboard(): DashboardResult {
        val settings = settingsStore.settings.first()
        if (settings.baseUrl.isBlank() || settings.token.isBlank()) return DashboardResult.SetupRequired
        return try {
            DashboardResult.Success(apiServiceProvider.create(settings.baseUrl, settings.token).dashboard().toDomain())
        } catch (error: HttpException) {
            when (error.code()) {
                401 -> DashboardResult.Unauthorized
                in 500..599 -> DashboardResult.ServerError
                else -> DashboardResult.ServerError
            }
        } catch (error: IOException) {
            DashboardResult.NetworkError
        }
    }
}
