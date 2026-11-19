package com.telesales.assistant.ui.calllog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telesales.assistant.data.network.FollowUpCreateDto
import com.telesales.assistant.data.network.InteractionCreateDto
import com.telesales.assistant.data.repository.CallLogRepository
import com.telesales.assistant.data.repository.CustomerDetailFailure
import com.telesales.assistant.data.repository.CustomerDetailResult
import com.telesales.assistant.data.repository.RecordInteractionResult
import com.telesales.assistant.domain.model.CustomerProfile
import com.telesales.assistant.reminder.ReminderAction
import com.telesales.assistant.reminder.ReminderCoordinator
import com.telesales.assistant.reminder.ReminderPlanner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class CallLogUiState(
    val customer: CustomerProfile? = null,
    val isLoading: Boolean = true,
    val loadError: CustomerDetailFailure? = null,
    val note: String = "",
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val validationErrors: Map<String, List<String>> = emptyMap(),
    val formError: String? = null,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
)

class CallLogViewModel(
    private val customerId: Long,
    private val repository: CallLogRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val reminderCoordinator: ReminderCoordinator? = null,
) : ViewModel() {
    private val mutableState = MutableStateFlow(CallLogUiState())
    val state = mutableState.asStateFlow()

    init { loadCustomer() }
    fun currentLocalDate(): LocalDate = clock.instant().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate()
    fun currentLocalTime(): LocalTime = clock.instant().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalTime().withSecond(0).withNano(0)
    fun setNote(value: String) { mutableState.value = mutableState.value.copy(note = value.take(500)) }
    fun setDate(value: LocalDate?) { mutableState.value = mutableState.value.copy(date = value, formError = null) }
    fun setTime(value: LocalTime?) { mutableState.value = mutableState.value.copy(time = value, formError = null) }
    fun clearSchedule() { mutableState.value = mutableState.value.copy(date = null, time = null, formError = null) }

    fun save() {
        val current = mutableState.value
        if (current.isSaving || current.saved) return
        if ((current.date == null) != (current.time == null)) {
            mutableState.value = current.copy(formError = "incomplete_schedule")
            return
        }
        submit(current)
    }

    private fun submit(current: CallLogUiState) {
        val remindAt = current.date?.let { date -> current.time?.let { time ->
            date.atTime(time).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant()
        } }
        if (remindAt != null && !remindAt.isAfter(clock.instant())) {
            mutableState.value = current.copy(formError = "past_schedule")
            return
        }
        mutableState.value = current.copy(isSaving = true, formError = null, validationErrors = emptyMap())
        viewModelScope.launch {
            val request = InteractionCreateDto(
                result = "contacted", needs = emptyList(),
                note = current.note.takeIf(String::isNotBlank),
                follow_up = remindAt?.let { FollowUpCreateDto(utcIso(it)) },
            )
            val result = try { repository.recordInteraction(customerId, request) }
            catch (cancelled: CancellationException) { throw cancelled }
            when (result) {
                is RecordInteractionResult.Success -> {
                    val reminder = result.followUpId?.let { id -> result.remindAt?.let { at ->
                        reminderCoordinator?.createReminder(
                            followUpId = id,
                            customerId = customerId,
                            customerName = current.customer?.contactName?.takeIf(String::isNotBlank)
                                ?: current.customer?.companyName?.takeIf(String::isNotBlank)
                                ?: current.customer?.phone.orEmpty(),
                            customerPhone = current.customer?.phone.orEmpty(),
                            remindAt = Instant.parse(at),
                            note = current.note.takeIf(String::isNotBlank),
                        )
                    } }
                    reminderCoordinator?.apply(ReminderPlanner.interaction(reminder, result.completedFollowUpIds))
                    mutableState.value = mutableState.value.copy(isSaving = false, saved = true)
                }
                is RecordInteractionResult.ValidationError -> mutableState.value = mutableState.value.copy(isSaving = false, validationErrors = result.fields)
                RecordInteractionResult.SetupRequired -> fail("setup")
                RecordInteractionResult.NetworkError -> fail("network")
                RecordInteractionResult.Unauthorized -> fail("unauthorized")
                RecordInteractionResult.ServerError -> fail("server")
            }
        }
    }

    private fun fail(message: String) { mutableState.value = mutableState.value.copy(isSaving = false, formError = message) }
    private fun loadCustomer() = viewModelScope.launch {
        when (val result = repository.getCallLogCustomer(customerId)) {
            is CustomerDetailResult.Success -> mutableState.value = mutableState.value.copy(customer = result.customer, isLoading = false)
            is CustomerDetailResult.Failure -> mutableState.value = mutableState.value.copy(loadError = result.reason, isLoading = false)
        }
    }

    companion object {
        fun utcIso(instant: Instant): String = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx")
            .withZone(ZoneId.of("UTC")).format(instant)
    }
}
