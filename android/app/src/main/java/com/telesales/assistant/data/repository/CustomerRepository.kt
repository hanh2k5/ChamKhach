package com.telesales.assistant.data.repository

import com.telesales.assistant.data.mapper.toDomain
import com.telesales.assistant.data.mapper.toCreateDto
import com.telesales.assistant.data.mapper.toUpdateDto
import com.telesales.assistant.data.network.ApiServiceProvider
import com.telesales.assistant.data.network.ApiErrorDto
import com.telesales.assistant.data.network.CustomerStatusDto
import com.telesales.assistant.data.network.InteractionCreateDto
import com.telesales.assistant.data.network.FollowUpCreateDto
import com.telesales.assistant.data.settings.SettingsStore
import com.telesales.assistant.domain.model.Customer
import com.telesales.assistant.domain.model.CustomerProfile
import com.telesales.assistant.domain.model.Interaction
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException

data class CustomerQuery(val text: String, val status: String, val potential: String?, val lastResult: String?)
data class CustomerPage(val customers: List<Customer>, val currentPage: Int, val lastPage: Int)
sealed interface CustomerLoadResult {
    data class Success(val page: CustomerPage) : CustomerLoadResult
    data object SetupRequired : CustomerLoadResult
    data object NetworkError : CustomerLoadResult
    data object Unauthorized : CustomerLoadResult
    data object ServerError : CustomerLoadResult
}

interface CustomerRepository {
    suspend fun getCustomers(query: CustomerQuery, page: Int): CustomerLoadResult
}

sealed interface CustomerFormResult {
    data class Success(val customer: CustomerProfile) : CustomerFormResult
    data class DuplicatePhone(val existingCustomerId: Long) : CustomerFormResult
    data class ValidationError(val fields: Map<String, List<String>>) : CustomerFormResult
    data object SetupRequired : CustomerFormResult
    data object NetworkError : CustomerFormResult
    data object Unauthorized : CustomerFormResult
    data object NotFound : CustomerFormResult
    data object ServerError : CustomerFormResult
}

interface CustomerFormRepository {
    suspend fun getCustomer(id: Long): CustomerFormResult
    suspend fun saveCustomer(id: Long?, customer: CustomerProfile): CustomerFormResult
}

enum class CustomerDetailFailure { SetupRequired, Network, Unauthorized, NotFound, Server }
sealed interface CustomerDetailResult {
    data class Success(val customer: CustomerProfile) : CustomerDetailResult
    data class Failure(val reason: CustomerDetailFailure) : CustomerDetailResult
}
data class InteractionPage(val items: List<Interaction>, val currentPage: Int, val lastPage: Int)
sealed interface InteractionPageResult {
    data class Success(val page: InteractionPage) : InteractionPageResult
    data class Failure(val reason: CustomerDetailFailure) : InteractionPageResult
}
sealed interface ArchiveCustomerResult {
    data class Success(val cancelledFollowUpIds: List<Long>) : ArchiveCustomerResult
    data class Failure(val reason: CustomerDetailFailure) : ArchiveCustomerResult
}
sealed interface RecordInteractionResult {
    data object Success : RecordInteractionResult
    data class ValidationError(val fields: Map<String, List<String>>) : RecordInteractionResult
    data object SetupRequired : RecordInteractionResult
    data object NetworkError : RecordInteractionResult
    data object Unauthorized : RecordInteractionResult
    data object ServerError : RecordInteractionResult
}
interface CallLogRepository {
    suspend fun getCallLogCustomer(id: Long): CustomerDetailResult
    suspend fun recordInteraction(id: Long, request: InteractionCreateDto): RecordInteractionResult
}

interface CustomerDetailRepository {
    suspend fun getCustomerDetail(id: Long): CustomerDetailResult
    suspend fun getInteractions(id: Long, page: Int): InteractionPageResult
    suspend fun archiveCustomer(id: Long): ArchiveCustomerResult
}

class ApiCustomerRepository(
    private val settingsStore: SettingsStore,
    private val apiServiceProvider: ApiServiceProvider,
) : CustomerRepository, CustomerFormRepository, CustomerDetailRepository, CallLogRepository {
    override suspend fun getCustomers(query: CustomerQuery, page: Int): CustomerLoadResult {
        val settings = settingsStore.settings.first()
        if (settings.baseUrl.isBlank() || settings.token.isBlank()) return CustomerLoadResult.SetupRequired
        return try {
            val response = apiServiceProvider.create(settings.baseUrl, settings.token).customers(
                query = query.text.takeIf(String::isNotBlank), status = query.status,
                potential = query.potential, lastResult = query.lastResult, page = page, perPage = 20,
            )
            CustomerLoadResult.Success(CustomerPage(response.data.map { it.toDomain() }, response.meta.current_page, response.meta.last_page))
        } catch (error: HttpException) {
            when (error.code()) {
                401 -> CustomerLoadResult.Unauthorized
                in 500..599 -> CustomerLoadResult.ServerError
                else -> CustomerLoadResult.ServerError
            }
        } catch (error: IOException) {
            CustomerLoadResult.NetworkError
        }
    }

    override suspend fun getCustomer(id: Long): CustomerFormResult = requestForm {
        apiServiceProvider.create(baseUrl, token).customer(id).data.toDomain()
    }

    override suspend fun saveCustomer(id: Long?, customer: CustomerProfile): CustomerFormResult = requestForm {
        val service = apiServiceProvider.create(baseUrl, token)
        if (id == null) service.createCustomer(customer.toCreateDto()).data.toDomain()
        else service.updateCustomer(id, customer.toUpdateDto()).data.toDomain()
    }

    override suspend fun getCustomerDetail(id: Long): CustomerDetailResult {
        val settings = settingsStore.settings.first()
        if (settings.baseUrl.isBlank() || settings.token.isBlank()) {
            return CustomerDetailResult.Failure(CustomerDetailFailure.SetupRequired)
        }
        return try {
            val profile = apiServiceProvider.create(settings.baseUrl, settings.token).customer(id).data
            CustomerDetailResult.Success(profile.toDomain())
        } catch (error: HttpException) {
            CustomerDetailResult.Failure(detailFailure(error.code()))
        } catch (error: IOException) {
            CustomerDetailResult.Failure(CustomerDetailFailure.Network)
        }
    }

    override suspend fun getInteractions(id: Long, page: Int): InteractionPageResult {
        val settings = settingsStore.settings.first()
        if (settings.baseUrl.isBlank() || settings.token.isBlank()) {
            return InteractionPageResult.Failure(CustomerDetailFailure.SetupRequired)
        }
        return try {
            val response = apiServiceProvider.create(settings.baseUrl, settings.token).interactions(id, page, 20)
            InteractionPageResult.Success(
                InteractionPage(response.data.map { it.toDomain() }, response.meta.current_page, response.meta.last_page),
            )
        } catch (error: HttpException) {
            InteractionPageResult.Failure(detailFailure(error.code()))
        } catch (error: IOException) {
            InteractionPageResult.Failure(CustomerDetailFailure.Network)
        }
    }

    override suspend fun archiveCustomer(id: Long): ArchiveCustomerResult {
        val settings = settingsStore.settings.first()
        if (settings.baseUrl.isBlank() || settings.token.isBlank()) {
            return ArchiveCustomerResult.Failure(CustomerDetailFailure.SetupRequired)
        }
        return try {
            val response = apiServiceProvider.create(settings.baseUrl, settings.token)
                .updateCustomerStatus(id, CustomerStatusDto(status = "archived"))
            ArchiveCustomerResult.Success(response.data.cancelled_follow_up_ids)
        } catch (error: HttpException) {
            ArchiveCustomerResult.Failure(detailFailure(error.code()))
        } catch (error: IOException) {
            ArchiveCustomerResult.Failure(CustomerDetailFailure.Network)
        }
    }

    override suspend fun getCallLogCustomer(id: Long) = getCustomerDetail(id)

    override suspend fun recordInteraction(id: Long, request: InteractionCreateDto): RecordInteractionResult {
        val settings = settingsStore.settings.first()
        if (settings.baseUrl.isBlank() || settings.token.isBlank()) return RecordInteractionResult.SetupRequired
        return try {
            apiServiceProvider.create(settings.baseUrl, settings.token).createInteraction(id, request)
            RecordInteractionResult.Success
        } catch (error: HttpException) {
            if (error.code() == 422) {
                val body = error.response()?.errorBody()?.string()
                val apiError = try { body?.let { Json { ignoreUnknownKeys = true }.decodeFromString<ApiErrorDto>(it) } }
                catch (_: SerializationException) { null }
                RecordInteractionResult.ValidationError(apiError?.errors.orEmpty())
            } else when (error.code()) {
                401 -> RecordInteractionResult.Unauthorized
                in 500..599 -> RecordInteractionResult.ServerError
                else -> RecordInteractionResult.ServerError
            }
        } catch (error: IOException) {
            RecordInteractionResult.NetworkError
        }
    }

    private fun detailFailure(statusCode: Int): CustomerDetailFailure = when (statusCode) {
        401 -> CustomerDetailFailure.Unauthorized
        404 -> CustomerDetailFailure.NotFound
        in 500..599 -> CustomerDetailFailure.Server
        else -> CustomerDetailFailure.Server
    }

    private suspend fun requestForm(request: suspend Credentials.() -> CustomerProfile): CustomerFormResult {
        val settings = settingsStore.settings.first()
        if (settings.baseUrl.isBlank() || settings.token.isBlank()) return CustomerFormResult.SetupRequired
        val credentials = Credentials(settings.baseUrl, settings.token)
        return try {
            CustomerFormResult.Success(credentials.request())
        } catch (error: HttpException) {
            mapFormHttpError(error)
        } catch (error: IOException) {
            CustomerFormResult.NetworkError
        }
    }

    private fun mapFormHttpError(error: HttpException): CustomerFormResult {
        val body = error.response()?.errorBody()?.string()
        val apiError = try {
            body?.let { Json { ignoreUnknownKeys = true }.decodeFromString<ApiErrorDto>(it) }
        } catch (_: SerializationException) {
            null
        }
        return when (error.code()) {
            401 -> CustomerFormResult.Unauthorized
            404 -> CustomerFormResult.NotFound
            409 -> if (apiError?.code == "DUPLICATE_PHONE" && apiError.existing_customer_id != null) {
                CustomerFormResult.DuplicatePhone(apiError.existing_customer_id)
            } else CustomerFormResult.ServerError
            422 -> CustomerFormResult.ValidationError(apiError?.errors.orEmpty())
            in 500..599 -> CustomerFormResult.ServerError
            else -> CustomerFormResult.ServerError
        }
    }

    private data class Credentials(val baseUrl: String, val token: String)
}
