package com.telesales.assistant.ui.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telesales.assistant.data.repository.CustomerLoadResult
import com.telesales.assistant.data.repository.CustomerQuery
import com.telesales.assistant.data.repository.CustomerRepository
import kotlinx.coroutines.CancellationException
import com.telesales.assistant.domain.model.Customer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CustomerError { SetupRequired, Network, Unauthorized, Server }
data class CustomersUiState(
    val customers: List<Customer> = emptyList(), val query: String = "",
    val potential: String? = null, val lastResult: String? = null, val isLoading: Boolean = true,
    val isRefreshing: Boolean = false, val currentPage: Int = 1, val lastPage: Int = 1,
    val error: CustomerError? = null,
)

class CustomersViewModel(
    private val repository: CustomerRepository,
    initialPotential: String? = null,
) : ViewModel() {
    private val mutableState = MutableStateFlow(CustomersUiState(potential = initialPotential))
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
        mutableState.value = mutableState.value.copy(query = value, customers = emptyList(),
            error = null, isLoading = true, isRefreshing = false, currentPage = 1, lastPage = 1)
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch { delay(300); loadFirstPage() }
    }

    fun onLastResultChanged(value: String?) = changeFilter { copy(lastResult = value) }
    fun retry() = resetAndLoad()
    fun refresh() = resetAndLoad(refresh = true)

    fun goToPage(targetPage: Int) {
        val current = mutableState.value
        if (targetPage !in 1..current.lastPage || current.isLoading || requestJob?.isActive == true) return
        val requestGeneration = generation
        mutableState.value = current.copy(customers = emptyList(), isLoading = true, error = null)
        requestJob = viewModelScope.launch {
            when (val result = repository.getCustomers(current.toQuery(), targetPage)) {
                is CustomerLoadResult.Success -> if (generation == requestGeneration) {
                    page = result.page.currentPage
                    mutableState.value = mutableState.value.copy(customers = result.page.customers,
                        currentPage = result.page.currentPage, lastPage = result.page.lastPage, isLoading = false)
                }
                else -> if (generation == requestGeneration) applyError(result)
            }
        }
    }

    private fun changeFilter(update: CustomersUiState.() -> CustomersUiState) {
        requestJob?.cancel()
        generation++
        page = 1
        mutableState.value = mutableState.value.update().copy(customers = emptyList(), isLoading = true,
            isRefreshing = false, currentPage = 1, lastPage = 1, error = null)
        debounceJob?.cancel()
        loadFirstPage()
    }

    private fun resetAndLoad(refresh: Boolean = false) {
        debounceJob?.cancel()
        requestJob?.cancel()
        generation++
        page = 1
        mutableState.value = mutableState.value.copy(customers = emptyList(), isLoading = !refresh,
            isRefreshing = refresh, currentPage = 1, lastPage = 1, error = null)
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
                        currentPage = result.page.currentPage, lastPage = result.page.lastPage, isLoading = false,
                        isRefreshing = false, error = null)
                }
                else -> applyError(result)
            }
        }
    }

    private fun applyError(result: CustomerLoadResult) {
        val error = when (result) {
            CustomerLoadResult.SetupRequired -> CustomerError.SetupRequired
            CustomerLoadResult.NetworkError -> CustomerError.Network
            CustomerLoadResult.Unauthorized -> CustomerError.Unauthorized
            CustomerLoadResult.ServerError -> CustomerError.Server
            is CustomerLoadResult.Success -> null
        }
        mutableState.value = mutableState.value.copy(isLoading = false, isRefreshing = false,
            error = error, customers = emptyList())
    }

    fun onPotentialChanged(value: String?) = changeFilter { copy(potential = value) }
    private fun CustomersUiState.toQuery() = CustomerQuery(query.trim(), potential, lastResult)
}
