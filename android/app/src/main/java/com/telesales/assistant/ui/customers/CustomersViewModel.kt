package com.telesales.assistant.ui.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telesales.assistant.data.repository.CustomerLoadResult
import com.telesales.assistant.data.repository.CustomerQuery
import com.telesales.assistant.data.repository.CustomerRepository
import com.telesales.assistant.domain.model.Customer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CustomerError { SetupRequired, Network, Unauthorized, Server }
data class CustomersUiState(
    val customers: List<Customer> = emptyList(), val query: String = "",
    val potential: String? = null, val lastResult: String? = null, val isLoading: Boolean = true,
    val isRefreshing: Boolean = false, val isLoadingMore: Boolean = false, val hasMore: Boolean = false,
    val error: CustomerError? = null,
)

class CustomersViewModel(
    private val repository: CustomerRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(CustomersUiState())
    val state: StateFlow<CustomersUiState> = mutableState.asStateFlow()
    private var requestJob: Job? = null
    private var debounceJob: Job? = null
    private var generation = 0L
    private var page = 1

    init { loadFirstPage() }

    fun onQueryChanged(value: String) {
        requestJob?.cancel()
        generation++
        page = 1
        mutableState.value = mutableState.value.copy(query = value, customers = emptyList(), hasMore = false,
            error = null, isLoading = true, isRefreshing = false, isLoadingMore = false)
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch { delay(300); loadFirstPage() }
    }

    fun onPotentialChanged(value: String?) = changeFilter { copy(potential = value) }
    fun onLastResultChanged(value: String?) = changeFilter { copy(lastResult = value) }
    fun retry() = resetAndLoad()
    fun refresh() = resetAndLoad(refresh = true)

    fun loadMore() {
        val current = mutableState.value
        if (current.isLoading || current.isLoadingMore || !current.hasMore || requestJob?.isActive == true) return
        val requestedPage = page + 1
        val requestGeneration = generation
        mutableState.value = current.copy(isLoadingMore = true)
        requestJob = viewModelScope.launch {
            when (val result = repository.getCustomers(current.toQuery(), requestedPage)) {
                is CustomerLoadResult.Success -> if (generation == requestGeneration) {
                    page = result.page.currentPage
                    mutableState.value = mutableState.value.copy(customers = mutableState.value.customers + result.page.customers,
                        hasMore = result.page.currentPage < result.page.lastPage, isLoadingMore = false)
                }
                else -> if (generation == requestGeneration) applyError(result, append = true)
            }
        }
    }

    private fun changeFilter(update: CustomersUiState.() -> CustomersUiState) {
        requestJob?.cancel()
        generation++
        page = 1
        mutableState.value = mutableState.value.update().copy(customers = emptyList(), isLoading = true,
            isRefreshing = false, isLoadingMore = false, hasMore = false, error = null)
        debounceJob?.cancel()
        loadFirstPage()
    }

    private fun resetAndLoad(refresh: Boolean = false) {
        debounceJob?.cancel()
        requestJob?.cancel()
        generation++
        page = 1
        mutableState.value = mutableState.value.copy(customers = emptyList(), isLoading = !refresh,
            isRefreshing = refresh, isLoadingMore = false, hasMore = false, error = null)
        loadFirstPage()
    }

    private fun loadFirstPage() {
        val requestGeneration = generation
        val query = mutableState.value.toQuery()
        requestJob = viewModelScope.launch {
            val result = try { repository.getCustomers(query, 1) } catch (cancelled: CancellationException) { throw cancelled }
            if (generation != requestGeneration) return@launch
            when (result) {
                is CustomerLoadResult.Success -> {
                    page = result.page.currentPage
                    mutableState.value = mutableState.value.copy(customers = result.page.customers,
                        hasMore = result.page.currentPage < result.page.lastPage, isLoading = false,
                        isRefreshing = false, error = null)
                }
                else -> applyError(result)
            }
        }
    }

    private fun applyError(result: CustomerLoadResult, append: Boolean = false) {
        val error = when (result) {
            CustomerLoadResult.SetupRequired -> CustomerError.SetupRequired
            CustomerLoadResult.NetworkError -> CustomerError.Network
            CustomerLoadResult.Unauthorized -> CustomerError.Unauthorized
            CustomerLoadResult.ServerError -> CustomerError.Server
            is CustomerLoadResult.Success -> null
        }
        mutableState.value = mutableState.value.copy(isLoading = false, isRefreshing = false,
            isLoadingMore = false, error = error, customers = if (append) mutableState.value.customers else emptyList())
    }

    private fun CustomersUiState.toQuery() = CustomerQuery(query.trim(), potential, lastResult)
}
