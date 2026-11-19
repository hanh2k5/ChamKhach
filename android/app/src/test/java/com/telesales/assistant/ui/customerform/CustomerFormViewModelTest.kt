package com.telesales.assistant.ui.customerform

import androidx.lifecycle.ViewModelStore
import com.telesales.assistant.data.network.InteractionCreateDto
import com.telesales.assistant.data.repository.CustomerFormRepository
import com.telesales.assistant.data.repository.CustomerFormResult
import com.telesales.assistant.data.repository.RecordInteractionResult
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerFormViewModelTest {
    private val stores = mutableListOf<ViewModelStore>()

    @Test fun defaultFollowUpTimeUsesCurrentVietnamTime() = formTest {
        val vm = create(FakeRepository(saveAction = { _, profile -> success(profile) }))
        assertEquals(LocalTime.of(7, 0), vm.currentLocalTime())
    }

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
        assertEquals("contacted", repository.interactionRequests.single().result)
        assertNull(repository.interactionRequests.single().follow_up)
        assertFalse(vm.state.value.initialInteractionPending)
    }

    @Test fun createSendsInitialCallNoteAndLimitsItToFiveHundredCharacters() = formTest {
        val repository = FakeRepository(saveAction = { _, profile -> success(profile.copy(id = 21)) })
        val vm = create(repository)
        vm.onPhoneChanged("0901234567")
        vm.onInitialNoteChanged("  Trao đổi về website  ")
        vm.save()
        advanceUntilIdle()

        assertEquals("Trao đổi về website", repository.interactionRequests.single().note)
        vm.onInitialNoteChanged("x".repeat(501))
        assertEquals(500, vm.state.value.initialNote.length)
    }

    @Test fun customerNeedsAreMultiSelectAndActiveSinceIsSaved() = formTest {
        val repository = FakeRepository(saveAction = { _, profile -> success(profile.copy(id = 15)) })
        val vm = create(repository)
        vm.onPhoneChanged("0901234567")
        vm.onActiveSinceChanged("5 năm")
        vm.toggleNeed("website")
        vm.toggleNeed("ads")
        vm.save()
        advanceUntilIdle()
        assertEquals("5 năm", repository.savedCustomer?.activeSince)
        assertEquals(setOf("website", "ads"), repository.savedCustomer?.needs?.toSet())
    }

    @Test fun createCanRecordTodayAndScheduleOptionalFutureFollowUp() = formTest {
        val repository = FakeRepository(saveAction = { _, profile -> success(profile.copy(id = 17)) })
        val vm = create(repository)
        vm.onPhoneChanged("0901234567")
        vm.setFollowUpDate(LocalDate.of(2026, 11, 3))
        vm.setFollowUpTime(LocalTime.of(9, 45))
        vm.save()
        advanceUntilIdle()

        assertEquals(17L, vm.state.value.savedCustomerId)
        assertEquals(1, repository.saveCalls)
        assertEquals("contacted", repository.interactionRequests.single().result)
        assertEquals("2026-11-03T02:45:00+00:00", repository.interactionRequests.single().follow_up?.remind_at)
    }

    @Test fun failedInitialInteractionRetryDoesNotCreateCustomerAgain() = formTest {
        var fail = true
        val repository = FakeRepository(
            saveAction = { _, profile -> success(profile.copy(id = 18)) },
            recordAction = { if (fail) RecordInteractionResult.NetworkError else RecordInteractionResult.Success(null, null, emptyList()) },
        )
        val vm = create(repository)
        vm.onPhoneChanged("0901234567")
        vm.save()
        advanceUntilIdle()
        assertEquals(18L, vm.state.value.customerId)
        assertTrue(vm.state.value.initialInteractionPending)
        assertEquals(1, repository.saveCalls)

        fail = false
        vm.save()
        advanceUntilIdle()
        assertEquals(18L, vm.state.value.savedCustomerId)
        assertEquals(1, repository.saveCalls)
        assertEquals(2, repository.interactionRequests.size)
    }

    @Test fun invalidInitialFollowUpDoesNotCreateCustomer() = formTest {
        val repository = FakeRepository(saveAction = { _, profile -> success(profile.copy(id = 19)) })
        val vm = create(repository)
        vm.onPhoneChanged("0901234567")
        vm.setFollowUpDate(LocalDate.of(2026, 11, 3))
        vm.save()
        advanceUntilIdle()
        assertEquals("incomplete_schedule", vm.state.value.formError)
        assertEquals(0, repository.saveCalls)
        assertTrue(repository.interactionRequests.isEmpty())
    }

    @Test fun pastInitialFollowUpDoesNotCreateCustomer() = formTest {
        val repository = FakeRepository(saveAction = { _, profile -> success(profile.copy(id = 20)) })
        val vm = create(repository)
        vm.onPhoneChanged("0901234567")
        vm.setFollowUpDate(LocalDate.of(2026, 11, 2))
        vm.setFollowUpTime(LocalTime.of(6, 59))
        vm.save()
        advanceUntilIdle()
        assertEquals("past_schedule", vm.state.value.formError)
        assertEquals(0, repository.saveCalls)
        assertTrue(repository.interactionRequests.isEmpty())
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
        CustomerFormViewModel(repository, customerId, Clock.fixed(Instant.parse("2026-11-02T00:00:00Z"), ZoneOffset.UTC)).also { viewModel ->
            ViewModelStore().also { store -> store.put("form-${stores.size}", viewModel); stores += store }
        }

    private fun formTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try { block() } finally { stores.forEach(ViewModelStore::clear); stores.clear(); Dispatchers.resetMain() }
    }

    private class FakeRepository(
        private val loadAction: suspend (Long) -> CustomerFormResult = { CustomerFormResult.NotFound },
        private val saveAction: suspend (Long?, CustomerProfile) -> CustomerFormResult,
        var recordAction: suspend () -> RecordInteractionResult = { RecordInteractionResult.Success(null, null, emptyList()) },
    ) : CustomerFormRepository {
        var saveCalls = 0
        var savedCustomer: CustomerProfile? = null
        var lastSavedId: Long? = null
        val interactionRequests = mutableListOf<InteractionCreateDto>()
        override suspend fun getCustomer(id: Long) = loadAction(id)
        override suspend fun saveCustomer(id: Long?, customer: CustomerProfile): CustomerFormResult {
            saveCalls++
            savedCustomer = customer
            lastSavedId = id
            return saveAction(id, customer)
        }
        override suspend fun recordInteraction(id: Long, request: InteractionCreateDto): RecordInteractionResult {
            interactionRequests += request
            return recordAction()
        }
    }

    private fun success(customer: CustomerProfile) = CustomerFormResult.Success(customer)
    private fun profile(id: Long) = CustomerProfile(id, "Acme", "Lan", "0912345678", null, null, "medium", null, "active")
}
