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
    val result: String? = null,
    val needs: Set<String> = setOf("other"),
    val note: String = "",
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val validationErrors: Map<String, List<String>> = emptyMap(),
    val formError: String? = null,
    val isSaving: Boolean = false,
    val missingScheduleDialog: Boolean = false,
    val saved: Boolean = false,
)

class CallLogViewModel(
    private val customerId: Long,
    private val repository: CallLogRepository,
    private val clock: Clock = Clock.systemUTC(),
) : ViewModel() {
    private val mutableState = MutableStateFlow(CallLogUiState())
    val state = mutableState.asStateFlow()

    init { loadCustomer() }
    fun chooseResult(value: String) { mutableState.value = mutableState.value.copy(result = value, validationErrors = emptyMap()) }
    fun toggleNeed(value: String) {
        val needs = mutableState.value.needs
        mutableState.value = mutableState.value.copy(needs = if (value in needs) {
            if (needs.size > 1) needs - value else needs
        } else needs + value)
    }
    fun setNote(value: String) { mutableState.value = mutableState.value.copy(note = value.take(500)) }
    fun setDate(value: LocalDate?) { mutableState.value = mutableState.value.copy(date = value, formError = null) }
    fun setTime(value: LocalTime?) { mutableState.value = mutableState.value.copy(time = value, formError = null) }
    fun clearSchedule() { mutableState.value = mutableState.value.copy(date = null, time = null, formError = null) }
    fun dismissScheduleDialog() { mutableState.value = mutableState.value.copy(missingScheduleDialog = false) }

    fun save() {
        val current = mutableState.value
        if (current.isSaving || current.saved) return
        if (current.result == null) {
            mutableState.value = current.copy(formError = "result")
            return
        }
        if (current.result == "callback" && (current.date == null || current.time == null)) {
            mutableState.value = current.copy(missingScheduleDialog = true, formError = null)
            return
        }
        if ((current.date == null) != (current.time == null)) {
            mutableState.value = current.copy(formError = "incomplete_schedule")
            return
        }
        submit(current)
    }

    fun saveWithoutSchedule() {
        val current = mutableState.value
        if (!current.isSaving && !current.saved && current.result == "callback") {
            mutableState.value = current.copy(missingScheduleDialog = false)
            submit(current.copy(date = null, time = null))
        }
    }

    private fun submit(current: CallLogUiState) {
        val remindAt = current.date?.let { date -> current.time?.let { time ->
            date.atTime(time).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant()
        } }
        if (remindAt != null && !remindAt.isAfter(clock.instant())) {
            mutableState.value = current.copy(formError = "past_schedule", missingScheduleDialog = false)
            return
        }
        mutableState.value = current.copy(isSaving = true, formError = null, validationErrors = emptyMap())
        viewModelScope.launch {
            val request = InteractionCreateDto(
                result = requireNotNull(current.result), needs = current.needs.toList(),
                note = current.note.takeIf(String::isNotBlank),
                follow_up = remindAt?.let { FollowUpCreateDto(utcIso(it)) },
            )
            val result = try { repository.recordInteraction(customerId, request) }
            catch (cancelled: CancellationException) { throw cancelled }
            when (result) {
                RecordInteractionResult.Success -> mutableState.value = mutableState.value.copy(isSaving = false, saved = true)
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
