package com.telesales.assistant.ui.customerdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telesales.assistant.data.repository.DeleteCustomerResult
import com.telesales.assistant.data.repository.CustomerDetailFailure
import com.telesales.assistant.data.repository.CustomerDetailRepository
import com.telesales.assistant.data.repository.CustomerDetailResult
import com.telesales.assistant.data.repository.InteractionPageResult
import com.telesales.assistant.domain.model.CustomerProfile
import com.telesales.assistant.domain.model.Interaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.telesales.assistant.reminder.ReminderCoordinator
import com.telesales.assistant.reminder.ReminderPlanner

data class CustomerDetailUiState(
    val customer: CustomerProfile? = null,
    val isLoading: Boolean = true,
    val error: CustomerDetailFailure? = null,
    val interactions: List<Interaction> = emptyList(),
    val isTimelineLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val timelineError: CustomerDetailFailure? = null,
    val deleteConfirmationVisible: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteError: CustomerDetailFailure? = null,
    val isDeleted: Boolean = false,
    val deletedFollowUpIds: List<Long> = emptyList(),
)

class CustomerDetailViewModel(
    private val customerId: Long,
    private val repository: CustomerDetailRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val zoneId: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh"),
    private val reminderCoordinator: ReminderCoordinator? = null,
) : ViewModel() {
    private val mutableState = MutableStateFlow(CustomerDetailUiState())
    val state: StateFlow<CustomerDetailUiState> = mutableState.asStateFlow()
    private var detailJob: Job? = null
    private var timelineJob: Job? = null
    private var generation = 0L
    private var page = 1

    init { load() }

    fun retry() = load()
    fun retryTimeline() = loadFirstTimelinePage()
    fun showDeleteConfirmation() { mutableState.value = mutableState.value.copy(deleteConfirmationVisible = true) }
    fun dismissDeleteConfirmation() { mutableState.value = mutableState.value.copy(deleteConfirmationVisible = false) }
    fun retryDelete() = deleteCustomer()

    fun formatTime(instant: Instant): String =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(clock.withZone(zoneId).zone)
            .format(instant)

    fun loadMore() {
        val current = mutableState.value
        if (current.isLoading || current.isTimelineLoading || current.isLoadingMore || !current.hasMore || current.timelineError != null) return
        val nextPage = page + 1
        val requestGeneration = generation
        mutableState.value = current.copy(isLoadingMore = true)
        timelineJob = viewModelScope.launch {
            val result = try { repository.getInteractions(customerId, nextPage) }
            catch (cancelled: CancellationException) { throw cancelled }
            if (requestGeneration != generation) return@launch
            when (result) {
                is InteractionPageResult.Success -> {
                    page = result.page.currentPage
                    mutableState.value = mutableState.value.copy(
                        interactions = mutableState.value.interactions + result.page.items,
                        hasMore = result.page.currentPage < result.page.lastPage,
                        isLoadingMore = false,
                    )
                }
                is InteractionPageResult.Failure -> mutableState.value = mutableState.value.copy(
                    isLoadingMore = false,
                    timelineError = result.reason,
                )
            }
        }
    }

    fun deleteCustomer() {
        if (mutableState.value.isDeleting || mutableState.value.isDeleted) return
        mutableState.value = mutableState.value.copy(isDeleting = true, deleteError = null)
        viewModelScope.launch {
            try {
                val result = try {
                    repository.deleteCustomer(customerId)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: IOException) {
                    DeleteCustomerResult.Failure(CustomerDetailFailure.Network)
                }

                when (result) {
                    is DeleteCustomerResult.Success -> {
                        reminderCoordinator?.apply(ReminderPlanner.delete(result.deletedFollowUpIds))
                        mutableState.value = mutableState.value.copy(
                            deleteConfirmationVisible = false,
                            isDeleted = true,
                            deletedFollowUpIds = result.deletedFollowUpIds,
                        )
                    }
                    is DeleteCustomerResult.Failure -> mutableState.value = mutableState.value.copy(
                        deleteError = result.reason,
                    )
                }
            } finally {
                mutableState.value = mutableState.value.copy(isDeleting = false)
            }
        }
    }

    private fun load() {
        detailJob?.cancel()
        timelineJob?.cancel()
        generation++
        page = 1
        val requestGeneration = generation
        mutableState.value = CustomerDetailUiState(isLoading = true, isTimelineLoading = true)
        detailJob = viewModelScope.launch {
            val result = try { repository.getCustomerDetail(customerId) }
            catch (cancelled: CancellationException) { throw cancelled }
            if (generation != requestGeneration) return@launch
            when (result) {
                is CustomerDetailResult.Success -> {
                    mutableState.value = mutableState.value.copy(customer = result.customer, isLoading = false)
                    loadFirstTimelinePage(requestGeneration)
                }
                is CustomerDetailResult.Failure -> mutableState.value = mutableState.value.copy(
                    isLoading = false,
                    isTimelineLoading = false,
                    error = result.reason,
                )
            }
        }
    }

    private fun loadFirstTimelinePage(expectedGeneration: Long? = null) {
        timelineJob?.cancel()
        page = 1
        val requestGeneration = expectedGeneration ?: generation
        mutableState.value = mutableState.value.copy(
            interactions = emptyList(),
            isTimelineLoading = true,
            isLoadingMore = false,
            hasMore = false,
            timelineError = null,
        )
        timelineJob = viewModelScope.launch {
            val result = try { repository.getInteractions(customerId, 1) }
            catch (cancelled: CancellationException) { throw cancelled }
            if (requestGeneration != generation) return@launch
            when (result) {
                is InteractionPageResult.Success -> {
                    page = result.page.currentPage
                    mutableState.value = mutableState.value.copy(
                        interactions = result.page.items,
                        isTimelineLoading = false,
                        hasMore = result.page.currentPage < result.page.lastPage,
                        timelineError = null,
                    )
                }
                is InteractionPageResult.Failure -> mutableState.value = mutableState.value.copy(
                    isTimelineLoading = false,
                    timelineError = result.reason,
                )
            }
        }
    }
}
