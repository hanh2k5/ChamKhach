package com.telesales.assistant.ui.customerform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telesales.assistant.data.repository.CustomerFormRepository
import com.telesales.assistant.data.repository.CustomerFormResult
import com.telesales.assistant.domain.model.CustomerProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class CustomerFormError { SetupRequired, Network, Unauthorized, NotFound, Server }

data class CustomerFormUiState(
    val customerId: Long? = null,
    val companyName: String = "",
    val contactName: String = "",
    val phone: String = "",
    val address: String = "",
    val industry: String = "",
    val source: String = "",
    val potentialLevel: String = "unrated",
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
) : ViewModel() {
    private val mutableState = MutableStateFlow(CustomerFormUiState(customerId = editingCustomerId, isLoading = editingCustomerId != null))
    val state: StateFlow<CustomerFormUiState> = mutableState.asStateFlow()

    init { if (editingCustomerId != null) loadCustomer() }

    fun onCompanyNameChanged(value: String) = update("company_name") { copy(companyName = value) }
    fun onContactNameChanged(value: String) = update("contact_name") { copy(contactName = value) }
    fun onPhoneChanged(value: String) = update("phone") { copy(phone = value) }
    fun onAddressChanged(value: String) = update("address") { copy(address = value) }
    fun onIndustryChanged(value: String) = update("industry") { copy(industry = value) }
    fun onSourceChanged(value: String) = update("source") { copy(source = value) }
    fun onPotentialLevelChanged(value: String) = update("potential_level") { copy(potentialLevel = value) }
    fun onStatusChanged(value: String) = update("status") { copy(status = value) }

    fun retryLoad() = loadCustomer()
    fun dismissDuplicate() { mutableState.value = mutableState.value.copy(duplicateCustomerId = null) }

    fun save() {
        val current = mutableState.value
        if (current.isSaving || current.isLoading) return
        val customer = current.toProfile()
        mutableState.value = current.copy(isSaving = true, fieldErrors = emptyMap(), error = null, duplicateCustomerId = null)
        viewModelScope.launch {
            val result = try { repository.saveCustomer(current.customerId, customer) }
            catch (cancelled: CancellationException) { throw cancelled }
            applyResult(result, saving = true)
        }
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
                industry = result.customer.industry.orEmpty(),
                source = result.customer.source.orEmpty(),
                potentialLevel = result.customer.potentialLevel,
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
        industry = industry.trim().ifBlank { null },
        source = source.trim().ifBlank { null },
        potentialLevel = potentialLevel,
        status = status,
    )
}
