package com.telesales.assistant.data.repository

import com.telesales.assistant.data.mapper.toDomain
import com.telesales.assistant.data.network.ApiErrorDto
import com.telesales.assistant.data.network.ApiServiceProvider
import com.telesales.assistant.data.network.FollowUpActionDto
import com.telesales.assistant.data.network.FollowUpDto
import com.telesales.assistant.data.settings.SettingsStore
import com.telesales.assistant.domain.model.FollowUp
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

data class FollowUpPage(val items: List<FollowUp>, val currentPage: Int, val lastPage: Int)

sealed interface FollowUpListResult {
    data class Success(val page: FollowUpPage) : FollowUpListResult
    data object SetupRequired : FollowUpListResult
    data object NetworkError : FollowUpListResult
    data object Unauthorized : FollowUpListResult
    data object ServerError : FollowUpListResult
}

sealed interface FollowUpActionResult {
    data class Success(val item: FollowUp) : FollowUpActionResult
    data object NotPending : FollowUpActionResult
    data class ValidationError(val fields: Map<String, List<String>>) : FollowUpActionResult
    data object SetupRequired : FollowUpActionResult
    data object NetworkError : FollowUpActionResult
    data object Unauthorized : FollowUpActionResult
    data object ServerError : FollowUpActionResult
}

interface FollowUpRepository {
    suspend fun getFollowUps(page: Int): FollowUpListResult
    suspend fun applyAction(id: Long, action: String, remindAt: String? = null): FollowUpActionResult
}

class ApiFollowUpRepository(
    private val settingsStore: SettingsStore,
    private val apiServiceProvider: ApiServiceProvider,
) : FollowUpRepository {
    override suspend fun getFollowUps(page: Int): FollowUpListResult {
        val settings = settingsStore.settings.first()
        if (settings.baseUrl.isBlank() || settings.token.isBlank()) return FollowUpListResult.SetupRequired
        return try {
            val response = apiServiceProvider.create(settings.baseUrl, settings.token)
                .followUps(status = "pending", group = "all", page = page, perPage = 20)
            FollowUpListResult.Success(FollowUpPage(response.data.map(FollowUpDto::toDomain), response.meta.current_page, response.meta.last_page))
        } catch (error: HttpException) {
            when (error.code()) {
                401 -> FollowUpListResult.Unauthorized
                in 500..599 -> FollowUpListResult.ServerError
                else -> FollowUpListResult.ServerError
            }
        } catch (error: IOException) {
            FollowUpListResult.NetworkError
        }
    }

    override suspend fun applyAction(id: Long, action: String, remindAt: String?): FollowUpActionResult {
        val settings = settingsStore.settings.first()
        if (settings.baseUrl.isBlank() || settings.token.isBlank()) return FollowUpActionResult.SetupRequired
        return try {
            val response = apiServiceProvider.create(settings.baseUrl, settings.token)
                .updateFollowUp(id, FollowUpActionDto(action, remindAt))
            FollowUpActionResult.Success(response.data.toDomain())
        } catch (error: HttpException) {
            val apiError = decodeError(error)
            when {
                error.code() == 401 -> FollowUpActionResult.Unauthorized
                error.code() == 409 && apiError?.code == "FOLLOW_UP_NOT_PENDING" -> FollowUpActionResult.NotPending
                error.code() == 422 -> FollowUpActionResult.ValidationError(apiError?.errors.orEmpty())
                else -> FollowUpActionResult.ServerError
            }
        } catch (error: IOException) {
            FollowUpActionResult.NetworkError
        }
    }

    private fun decodeError(error: HttpException): ApiErrorDto? = try {
        error.response()?.errorBody()?.string()?.let { Json { ignoreUnknownKeys = true }.decodeFromString<ApiErrorDto>(it) }
    } catch (_: SerializationException) {
        null
    }
}
