package com.telesales.assistant.data.repository

import com.telesales.assistant.data.mapper.toDomain
import com.telesales.assistant.data.network.ApiServiceProvider
import com.telesales.assistant.data.settings.SettingsStore
import com.telesales.assistant.domain.model.Customer
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

class ApiCustomerRepository(
    private val settingsStore: SettingsStore,
    private val apiServiceProvider: ApiServiceProvider,
) : CustomerRepository {
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
}
