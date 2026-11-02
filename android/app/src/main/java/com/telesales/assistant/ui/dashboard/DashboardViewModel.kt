package com.telesales.assistant.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telesales.assistant.data.repository.DashboardRepository
import com.telesales.assistant.data.repository.DashboardResult
import com.telesales.assistant.domain.model.DashboardSummary
import com.telesales.assistant.domain.model.FollowUp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.ZoneId

enum class DashboardError { SetupRequired, Network, Unauthorized, Server }

data class DashboardUiState(
    val dashboard: DashboardSummary? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: DashboardError? = null,
)

class DashboardViewModel(
    private val repository: DashboardRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val businessZone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh"),
) : ViewModel() {
    private val mutableState = MutableStateFlow(DashboardUiState())
    val state = mutableState.asStateFlow()
    private var requestJob: Job? = null
    private var generation = 0L

    init { load() }
    fun retry() = load()
    fun refresh() = load(refreshing = true)
    fun formatTime(item: FollowUp): String = item.formattedRemindAt(clock, businessZone)

    private fun load(refreshing: Boolean = false) {
        requestJob?.cancel()
        val requestGeneration = ++generation
        mutableState.value = mutableState.value.copy(
            isLoading = !refreshing,
            isRefreshing = refreshing,
            error = null,
        )
        requestJob = viewModelScope.launch {
            val result = try { repository.getDashboard() } catch (cancelled: CancellationException) { throw cancelled }
            if (generation != requestGeneration) return@launch
            when (result) {
                is DashboardResult.Success -> mutableState.value = DashboardUiState(
                    dashboard = result.dashboard,
                    isLoading = false,
                    isRefreshing = false,
                )
                DashboardResult.SetupRequired -> fail(DashboardError.SetupRequired)
                DashboardResult.NetworkError -> fail(DashboardError.Network)
                DashboardResult.Unauthorized -> fail(DashboardError.Unauthorized)
                DashboardResult.ServerError -> fail(DashboardError.Server)
            }
        }
    }

    private fun fail(error: DashboardError) {
        mutableState.value = mutableState.value.copy(isLoading = false, isRefreshing = false, error = error)
    }
}
