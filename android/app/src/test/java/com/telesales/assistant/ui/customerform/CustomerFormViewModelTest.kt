package com.telesales.assistant.ui.customerform

import androidx.lifecycle.ViewModelStore
import com.telesales.assistant.data.repository.CustomerFormRepository
import com.telesales.assistant.data.repository.CustomerFormResult
import com.telesales.assistant.domain.model.CustomerProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerFormViewModelTest {
    private val stores = mutableListOf<ViewModelStore>()

    @Test fun createSavesOptionalFieldsAndReturnsCreatedId() = formTest {
        val repository = FakeRepository(saveAction = { _, profile -> success(profile.copy(id = 14)) })
        val vm = create(repository)
        vm.onPhoneChanged("0901234567")
        vm.onCompanyNameChanged("Công ty")
        vm.save()
        advanceUntilIdle()

        assertEquals(14L, vm.state.value.savedCustomerId)
        assertEquals(null, repository.savedCustomer?.address)
        assertEquals(null, repository.lastSavedId)
    }

    @Test fun editLoadsProfileAndSavesWithPatchId() = formTest {
        val repository = FakeRepository(
            loadAction = { id -> success(profile(id).copy(companyName = "Khách cũ")) },
            saveAction = { id, customer -> success(customer.copy(id = id ?: -1)) },
        )
        val vm = create(repository, customerId = 7)
        advanceUntilIdle()
        assertEquals("Khách cũ", vm.state.value.companyName)
        vm.onPhoneChanged("0912345678")
        vm.save()
        advanceUntilIdle()
        assertEquals(7L, repository.lastSavedId)
    }

    @Test fun maps422ErrorsToTheirFields() = formTest {
        val repository = FakeRepository(saveAction = { _, _ -> CustomerFormResult.ValidationError(mapOf("phone" to listOf("invalid"))) })
        val vm = create(repository)
        vm.onPhoneChanged("bad")
        vm.save()
        advanceUntilIdle()
        assertEquals(listOf("invalid"), vm.state.value.fieldErrors["phone"])
        vm.onPhoneChanged("0901234567")
        assertFalse(vm.state.value.fieldErrors.containsKey("phone"))
    }

    @Test fun duplicatePhoneProvidesExistingCustomerIdAndCanDismiss() = formTest {
        val repository = FakeRepository(saveAction = { _, _ -> CustomerFormResult.DuplicatePhone(23) })
        val vm = create(repository)
        vm.onPhoneChanged("0901234567")
        vm.save()
        advanceUntilIdle()
        assertEquals(23L, vm.state.value.duplicateCustomerId)
        vm.dismissDuplicate()
        assertEquals(null, vm.state.value.duplicateCustomerId)
    }

    @Test fun doubleSaveWhileRequestIsRunningCreatesOneRequest() = formTest {
        val repository = FakeRepository(saveAction = { _, customer -> delay(100); success(customer.copy(id = 3)) })
        val vm = create(repository)
        vm.onPhoneChanged("0901234567")
        vm.save()
        vm.save()
        advanceUntilIdle()
        assertEquals(1, repository.saveCalls)
        assertFalse(vm.state.value.isSaving)
    }

    private fun TestScope.create(repository: FakeRepository, customerId: Long? = null) =
        CustomerFormViewModel(repository, customerId).also { viewModel ->
            ViewModelStore().also { store -> store.put("form-${stores.size}", viewModel); stores += store }
        }

    private fun formTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try { block() } finally { stores.forEach(ViewModelStore::clear); stores.clear(); Dispatchers.resetMain() }
    }

    private class FakeRepository(
        private val loadAction: suspend (Long) -> CustomerFormResult = { CustomerFormResult.NotFound },
        private val saveAction: suspend (Long?, CustomerProfile) -> CustomerFormResult,
    ) : CustomerFormRepository {
        var saveCalls = 0
        var savedCustomer: CustomerProfile? = null
        var lastSavedId: Long? = null
        override suspend fun getCustomer(id: Long) = loadAction(id)
        override suspend fun saveCustomer(id: Long?, customer: CustomerProfile): CustomerFormResult {
            saveCalls++
            savedCustomer = customer
            lastSavedId = id
            return saveAction(id, customer)
        }
    }

    private fun success(customer: CustomerProfile) = CustomerFormResult.Success(customer)
    private fun profile(id: Long) = CustomerProfile(id, "Acme", "Lan", "0912345678", null, null, null, "unrated", "active")
}
