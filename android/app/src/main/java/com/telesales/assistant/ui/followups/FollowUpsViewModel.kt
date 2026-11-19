package com.telesales.assistant.ui.followups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telesales.assistant.data.repository.FollowUpActionResult
import com.telesales.assistant.data.repository.FollowUpListResult
import com.telesales.assistant.data.repository.FollowUpRepository
import com.telesales.assistant.domain.model.FollowUp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.telesales.assistant.reminder.ReminderAction
import com.telesales.assistant.reminder.ReminderCoordinator
import com.telesales.assistant.reminder.ReminderPlanner

enum class FollowUpsError { SetupRequired, Network, Unauthorized, Server }

data class FollowUpsUiState(
    val items: List<FollowUp> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val currentPage: Int = 1,
    val lastPage: Int = 1,
    val error: FollowUpsError? = null,
    val busyIds: Set<Long> = emptySet(),
    val actionError: FollowUpsError? = null,
    val validationErrors: Map<String, List<String>> = emptyMap(),
    val notice: String? = null,
    val rescheduleForId: Long? = null,
    val rescheduleDate: LocalDate? = null,
    val rescheduleTime: LocalTime? = null,
    val rescheduleError: String? = null,
)

class FollowUpsViewModel(
    private val repository: FollowUpRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val businessZone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh"),
    private val reminderCoordinator: ReminderCoordinator? = null,
    private val initialGroup: String = "all",
) : ViewModel() {
    private val mutableState = MutableStateFlow(FollowUpsUiState())
    val state = mutableState.asStateFlow()
    private var listJob: Job? = null
    private var syncJob: Job? = null
    private var generation = 0L
    private var page = 1
    private var initialLoaded = false

    init { load() }
    fun retry() = load()
    fun refresh() = load(refreshing = true)
    fun dismissNotice() { mutableState.value = mutableState.value.copy(notice = null) }

    fun goToPage(targetPage: Int) {
        val current = mutableState.value
        if (targetPage !in 1..current.lastPage || current.isLoading || current.isRefreshing) return
        val requestGeneration = generation
        mutableState.value = current.copy(items = emptyList(), isLoading = true, error = null)
        listJob = viewModelScope.launch {
            val result = try { repository.getFollowUps(targetPage, initialGroup) } catch (cancelled: CancellationException) { throw cancelled }
            if (generation != requestGeneration) return@launch
            when (result) {
                is FollowUpListResult.Success -> {
                    page = result.page.currentPage
                    mutableState.value = mutableState.value.copy(
                        items = sortByServerGroup(result.page.items),
                        currentPage = result.page.currentPage,
                        lastPage = result.page.lastPage,
                        isLoading = false,
                        error = null,
                    )
                }
                else -> applyListError(result)
            }
        }
    }

    fun complete(item: FollowUp) = apply(item, "complete")

    fun beginReschedule(item: FollowUp) {
        if (item.id in mutableState.value.busyIds) return
        mutableState.value = mutableState.value.copy(
            rescheduleForId = item.id,
            rescheduleDate = item.remindAt.atZone(businessZone).toLocalDate(),
            rescheduleTime = item.remindAt.atZone(businessZone).toLocalTime(),
            rescheduleError = null,
            validationErrors = emptyMap(),
            actionError = null,
        )
    }

    fun setRescheduleDate(value: LocalDate?) { mutableState.value = mutableState.value.copy(rescheduleDate = value, rescheduleError = null) }
    fun setRescheduleTime(value: LocalTime?) { mutableState.value = mutableState.value.copy(rescheduleTime = value, rescheduleError = null) }
    fun dismissReschedule() { mutableState.value = mutableState.value.copy(rescheduleForId = null, rescheduleError = null) }

    fun submitReschedule() {
        val current = mutableState.value
        val id = current.rescheduleForId ?: return
        val date = current.rescheduleDate
        val time = current.rescheduleTime
        if (date == null || time == null) {
            mutableState.value = current.copy(rescheduleError = "incomplete")
            return
        }
        val instant = date.atTime(time).atZone(businessZone).toInstant()
        if (!instant.isAfter(clock.instant())) {
            mutableState.value = current.copy(rescheduleError = "past")
            return
        }
        val item = current.items.firstOrNull { it.id == id } ?: return
        apply(item, "reschedule", utcIso(instant))
    }

    fun formatTime(item: FollowUp): String = item.formattedRemindAt(clock, businessZone)

    private fun apply(item: FollowUp, action: String, remindAt: String? = null) {
        val current = mutableState.value
        if (item.id in current.busyIds) return
        mutableState.value = current.copy(busyIds = current.busyIds + item.id, actionError = null, validationErrors = emptyMap(), rescheduleError = null)
        viewModelScope.launch {
            val result = try { repository.applyAction(item.id, action, remindAt) }
            catch (cancelled: CancellationException) { throw cancelled }
            val state = mutableState.value.copy(busyIds = mutableState.value.busyIds - item.id)
            when (result) {
                is FollowUpActionResult.Success -> {
                    reminderCoordinator?.apply(
                        if (action == "reschedule") listOf(ReminderAction.Schedule(reminderCoordinator.toReminder(result.item)))
                        else ReminderPlanner.completeOrCancel(item.id),
                    )
                    val updatedItems = if (action == "complete") state.items.filterNot { it.id == item.id }
                    else state.items.map { if (it.id == item.id) result.item else it }
                    mutableState.value = state.copy(items = sortByServerGroup(updatedItems), rescheduleForId = if (action == "reschedule") null else state.rescheduleForId)
                }
                FollowUpActionResult.NotPending -> {
                    mutableState.value = state.copy(notice = "conflict")
                    load(refreshing = true, keepNotice = true)
                }
                is FollowUpActionResult.ValidationError -> mutableState.value = state.copy(validationErrors = result.fields)
                FollowUpActionResult.SetupRequired -> mutableState.value = state.copy(actionError = FollowUpsError.SetupRequired)
                FollowUpActionResult.NetworkError -> mutableState.value = state.copy(actionError = FollowUpsError.Network)
                FollowUpActionResult.Unauthorized -> mutableState.value = state.copy(actionError = FollowUpsError.Unauthorized)
                FollowUpActionResult.ServerError -> mutableState.value = state.copy(actionError = FollowUpsError.Server)
            }
        }
    }

    private fun load(refreshing: Boolean = false, keepNotice: Boolean = false) {
        listJob?.cancel()
        syncJob?.cancel()
        generation++
        page = 1
        val requestGeneration = generation
        val notice = if (keepNotice) mutableState.value.notice else null
        val visibleItems = if (refreshing) mutableState.value.items else emptyList()
        val visiblePage = if (refreshing) mutableState.value.currentPage else 1
        val visibleLastPage = if (refreshing) mutableState.value.lastPage else 1
        mutableState.value = FollowUpsUiState(
            items = visibleItems,
            isLoading = !refreshing,
            isRefreshing = refreshing,
            currentPage = visiblePage,
            lastPage = visibleLastPage,
            notice = notice,
        )
        listJob = viewModelScope.launch {
            val result = try { repository.getFollowUps(visiblePage, initialGroup) } catch (cancelled: CancellationException) { throw cancelled }
            if (generation != requestGeneration) return@launch
            when (result) {
                is FollowUpListResult.Success -> {
                    page = result.page.currentPage
                    mutableState.value = mutableState.value.copy(
                        items = sortByServerGroup(result.page.items),
                        isLoading = false,
                        isRefreshing = false,
                        currentPage = result.page.currentPage,
                        lastPage = result.page.lastPage,
                        error = null,
                    )
                    // Chỉ sync lần đầu load, không sync khi refresh để tránh chậm UI
                    if (!initialLoaded) {
                        initialLoaded = true
                        syncJob = viewModelScope.launch(Dispatchers.IO) {
                            reminderCoordinator?.sync(repository)
                        }
                    }
                }
                else -> applyListError(result)
            }
        }
    }

    private fun applyListError(result: FollowUpListResult) {
        val error = when (result) {
            FollowUpListResult.SetupRequired -> FollowUpsError.SetupRequired
            FollowUpListResult.NetworkError -> FollowUpsError.Network
            FollowUpListResult.Unauthorized -> FollowUpsError.Unauthorized
            FollowUpListResult.ServerError -> FollowUpsError.Server
            is FollowUpListResult.Success -> null
        }
        mutableState.value = mutableState.value.copy(
            isLoading = false,
            isRefreshing = false,
            error = error,
            items = emptyList(),
        )
    }

    private fun sortByServerGroup(items: List<FollowUp>) = items.sortedWith(
        compareBy<FollowUp> { groupOrder(it.group) }.thenBy(FollowUp::remindAt),
    )

    private fun groupOrder(group: String) = when (group) { "overdue" -> 0; "today" -> 1; "upcoming" -> 2; else -> 3 }

    companion object {
        fun utcIso(instant: Instant): String = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx")
            .withZone(ZoneId.of("UTC")).format(instant)
    }
}
