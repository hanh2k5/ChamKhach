package com.telesales.assistant.data.repository

import com.telesales.assistant.data.mapper.toDomain
import com.telesales.assistant.data.mapper.toCreateDto
import com.telesales.assistant.data.mapper.toUpdateDto
import com.telesales.assistant.data.network.ApiServiceProvider
import com.telesales.assistant.data.network.ApiErrorDto
import com.telesales.assistant.data.settings.SettingsStore
import com.telesales.assistant.domain.model.Customer
import com.telesales.assistant.domain.model.CustomerProfile
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

class ApiCustomerRepository(
    private val settingsStore: SettingsStore,
    private val apiServiceProvider: ApiServiceProvider,
) : CustomerRepository, CustomerFormRepository {
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
