package com.telesales.assistant.ui.customerform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telesales.assistant.data.network.FollowUpCreateDto
import com.telesales.assistant.data.network.InteractionCreateDto
import com.telesales.assistant.data.repository.CustomerFormRepository
import com.telesales.assistant.data.repository.CustomerFormResult
import com.telesales.assistant.data.repository.RecordInteractionResult
import com.telesales.assistant.domain.model.CustomerProfile
import com.telesales.assistant.reminder.ReminderCoordinator
import com.telesales.assistant.reminder.ReminderPlanner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class CustomerFormError { SetupRequired, Network, Unauthorized, NotFound, Server }

data class CustomerFormUiState(
    val customerId: Long? = null,
    val companyName: String = "",
    val contactName: String = "",
    val phone: String = "",
    val address: String = "",
    val activeSince: String = "",
    val industry: String = "",
    val needs: Set<String> = emptySet(),
    val initialNote: String = "",
    val source: String = "",
    val potentialLevel: String = "medium",
    val followUpDate: LocalDate? = null,
    val followUpTime: LocalTime? = null,
    val initialInteractionPending: Boolean = false,
    val formError: String? = null,
    val status: String = "active",
    val isLoading: Boolean = false,
    val isLoadFailure: Boolean = false,
    val isSaving: Boolean = false,
    val fieldErrors: Map<String, List<String>> = emptyMap(),
    val error: CustomerFormError? = null,
    val duplicateCustomerId: Long? = null,
    val savedCustomerId: Long? = null,
)

class CustomerFormViewModel(
    private val repository: CustomerFormRepository,
    private val editingCustomerId: Long?,
    private val clock: Clock = Clock.systemUTC(),
    private val reminderCoordinator: ReminderCoordinator? = null,
) : ViewModel() {
    private val mutableState = MutableStateFlow(CustomerFormUiState(customerId = editingCustomerId, isLoading = editingCustomerId != null))
    val state: StateFlow<CustomerFormUiState> = mutableState.asStateFlow()

    init { if (editingCustomerId != null) loadCustomer() }

    fun currentLocalTime(): LocalTime = clock.instant()
        .atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
        .toLocalTime()
        .withSecond(0)
        .withNano(0)

    fun currentLocalDate(): LocalDate = clock.instant()
        .atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
        .toLocalDate()

    fun onCompanyNameChanged(value: String) = update("company_name") { copy(companyName = value) }
    fun onContactNameChanged(value: String) = update("contact_name") { copy(contactName = value) }
    fun onPhoneChanged(value: String) = update("phone") { copy(phone = value) }
    fun onAddressChanged(value: String) = update("address") { copy(address = value) }
    fun onActiveSinceChanged(value: String) = update("active_since") { copy(activeSince = value) }
    fun onIndustryChanged(value: String) = update("industry") { copy(industry = value) }
    fun onInitialNoteChanged(value: String) { mutableState.value = mutableState.value.copy(initialNote = value.take(500)) }
    fun onPotentialLevelChanged(value: String) {
        if (value == "high" || value == "medium") update("potential_level") { copy(potentialLevel = value) }
    }
    fun setFollowUpDate(value: LocalDate?) { mutableState.value = mutableState.value.copy(followUpDate = value, formError = null) }
    fun setFollowUpTime(value: LocalTime?) { mutableState.value = mutableState.value.copy(followUpTime = value, formError = null) }
    fun clearFollowUp() { mutableState.value = mutableState.value.copy(followUpDate = null, followUpTime = null, formError = null) }
    fun toggleNeed(value: String) {
        val current = mutableState.value
        val selected = current.needs
        mutableState.value = current.copy(needs = if (value in selected) selected - value else selected + value)
    }

    fun retryLoad() = loadCustomer()
    fun dismissDuplicate() { mutableState.value = mutableState.value.copy(duplicateCustomerId = null) }

    fun save() {
        val current = mutableState.value
        if (current.isSaving || current.isLoading) return
        if (editingCustomerId == null && !validSchedule(current)) return
        if (editingCustomerId == null && current.initialInteractionPending && current.customerId != null) {
            recordInitialInteraction(current, current.customerId)
            return
        }
        val customer = current.toProfile()
        mutableState.value = current.copy(isSaving = true, fieldErrors = emptyMap(), error = null, duplicateCustomerId = null)
        viewModelScope.launch {
            val result = try { repository.saveCustomer(current.customerId, customer) }
            catch (cancelled: CancellationException) { throw cancelled }
            if (editingCustomerId == null && result is CustomerFormResult.Success) {
                mutableState.value = mutableState.value.copy(
                    customerId = result.customer.id,
                    initialInteractionPending = true,
                    error = null,
                )
                recordInitialInteraction(mutableState.value, result.customer.id)
            } else {
                applyResult(result, saving = true)
            }
        }
    }

    private fun validSchedule(current: CustomerFormUiState): Boolean {
        if ((current.followUpDate == null) != (current.followUpTime == null)) {
            mutableState.value = current.copy(formError = "incomplete_schedule")
            return false
        }
        val remindAt = scheduledAt(current)
        if (remindAt != null && !remindAt.isAfter(clock.instant())) {
            mutableState.value = current.copy(formError = "past_schedule")
            return false
        }
        return true
    }

    private fun recordInitialInteraction(current: CustomerFormUiState, customerId: Long) {
        mutableState.value = current.copy(isSaving = true, error = null, fieldErrors = emptyMap(), formError = null)
        viewModelScope.launch {
            val remindAt = scheduledAt(current)
            val result = try {
                repository.recordInteraction(
                    customerId,
                    InteractionCreateDto(
                        result = "contacted",
                        needs = emptyList(),
                        note = current.initialNote.trim().takeIf(String::isNotEmpty),
                        follow_up = remindAt?.let { FollowUpCreateDto(utcIso(it)) },
                    ),
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            }
            when (result) {
                is RecordInteractionResult.Success -> {
                    val reminder = result.followUpId?.let { id -> result.remindAt?.let { at ->
                        reminderCoordinator?.createReminder(
                            followUpId = id,
                            customerId = customerId,
                            customerName = current.contactName.takeIf(String::isNotBlank)
                                ?: current.companyName.takeIf(String::isNotBlank)
                                ?: current.phone,
                            customerPhone = current.phone,
                            remindAt = Instant.parse(at),
                            note = current.initialNote.trim().takeIf(String::isNotEmpty),
                        )
                    } }
                    reminderCoordinator?.apply(ReminderPlanner.interaction(reminder, result.completedFollowUpIds))
                    mutableState.value = mutableState.value.copy(
                        customerId = customerId,
                        isSaving = false,
                        initialInteractionPending = false,
                        savedCustomerId = customerId,
                        formError = null,
                    )
                }
                is RecordInteractionResult.ValidationError -> {
                    val pastTime = "follow_up.remind_at" in result.fields
                    mutableState.value = mutableState.value.copy(
                        isSaving = false,
                        fieldErrors = result.fields,
                        formError = if (pastTime) "past_schedule" else null,
                    )
                }
                RecordInteractionResult.SetupRequired -> initialInteractionFailed(CustomerFormError.SetupRequired)
                RecordInteractionResult.NetworkError -> initialInteractionFailed(CustomerFormError.Network)
                RecordInteractionResult.Unauthorized -> initialInteractionFailed(CustomerFormError.Unauthorized)
                RecordInteractionResult.ServerError -> initialInteractionFailed(CustomerFormError.Server)
            }
        }
    }

    private fun initialInteractionFailed(error: CustomerFormError) {
        mutableState.value = mutableState.value.copy(isSaving = false, error = error, initialInteractionPending = true)
    }

    private fun scheduledAt(current: CustomerFormUiState): Instant? = current.followUpDate?.let { date ->
        current.followUpTime?.let { time -> date.atTime(time).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant() }
    }

    private fun loadCustomer() {
        val id = editingCustomerId ?: return
        mutableState.value = mutableState.value.copy(isLoading = true, isLoadFailure = false, error = null)
        viewModelScope.launch {
            val result = try { repository.getCustomer(id) }
            catch (cancelled: CancellationException) { throw cancelled }
            applyResult(result, saving = false)
        }
    }

    private fun applyResult(result: CustomerFormResult, saving: Boolean) {
        val current = mutableState.value
        when (result) {
            is CustomerFormResult.Success -> mutableState.value = if (saving) {
                current.copy(customerId = result.customer.id, isSaving = false, isLoading = false, savedCustomerId = result.customer.id)
            } else current.copy(
                customerId = result.customer.id,
                companyName = result.customer.companyName.orEmpty(),
                contactName = result.customer.contactName.orEmpty(),
                phone = result.customer.phone,
                address = result.customer.address.orEmpty(),
                activeSince = result.customer.activeSince.orEmpty(),
                industry = result.customer.industry.orEmpty(),
                needs = result.customer.needs.toSet(),
                source = result.customer.source.orEmpty(),
                potentialLevel = result.customer.potentialLevel.takeIf { it == "high" || it == "medium" } ?: "medium",
                status = result.customer.status,
                isLoading = false,
                error = null,
            )
            is CustomerFormResult.DuplicatePhone -> mutableState.value = current.copy(
                isSaving = false, duplicateCustomerId = result.existingCustomerId,
            )
            is CustomerFormResult.ValidationError -> mutableState.value = current.copy(
                isLoading = false, isSaving = false, fieldErrors = result.fields,
            )
            CustomerFormResult.SetupRequired -> applyFailure(CustomerFormError.SetupRequired, loadFailure = !saving)
            CustomerFormResult.NetworkError -> applyFailure(CustomerFormError.Network, loadFailure = !saving)
            CustomerFormResult.Unauthorized -> applyFailure(CustomerFormError.Unauthorized, loadFailure = !saving)
            CustomerFormResult.NotFound -> applyFailure(CustomerFormError.NotFound, loadFailure = !saving)
            CustomerFormResult.ServerError -> applyFailure(CustomerFormError.Server, loadFailure = !saving)
        }
    }

    private fun applyFailure(error: CustomerFormError, loadFailure: Boolean) {
        mutableState.value = mutableState.value.copy(isLoading = false, isSaving = false, isLoadFailure = loadFailure, error = error)
    }

    private fun update(field: String, transform: CustomerFormUiState.() -> CustomerFormUiState) {
        val current = mutableState.value
        mutableState.value = current.transform().copy(
            fieldErrors = current.fieldErrors - field,
            error = null,
        )
    }

    private fun CustomerFormUiState.toProfile() = CustomerProfile(
        id = customerId ?: 0,
        companyName = companyName.trim().ifBlank { null },
        contactName = contactName.trim().ifBlank { null },
        phone = phone.trim(),
        address = address.trim().ifBlank { null },
        activeSince = activeSince.trim().ifBlank { null },
        industry = industry.trim().ifBlank { null },
        needs = needs.toList(),
        source = source.trim().ifBlank { null },
        potentialLevel = potentialLevel,
        status = status,
    )

    private fun utcIso(instant: Instant): String = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx")
        .withZone(ZoneId.of("UTC")).format(instant)
}
