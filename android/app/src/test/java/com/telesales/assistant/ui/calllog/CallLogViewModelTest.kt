package com.telesales.assistant.ui.calllog

import androidx.lifecycle.ViewModelStore
import com.telesales.assistant.data.network.InteractionCreateDto
import com.telesales.assistant.data.repository.CallLogRepository
import com.telesales.assistant.data.repository.CustomerDetailFailure
import com.telesales.assistant.data.repository.CustomerDetailResult
import com.telesales.assistant.data.repository.RecordInteractionResult
import com.telesales.assistant.domain.model.CustomerProfile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class CallLogViewModelTest {
    private val stores = mutableListOf<ViewModelStore>()

    @Test fun callLogDefaultsToContactedWithNoNeedsAndNoSchedule() = vmTest {
        val repo = FakeRepository(); val vm = create(repo); advanceUntilIdle()
        vm.save(); advanceUntilIdle()
        assertEquals("contacted", repo.requests.single().result)
        assertTrue(repo.requests.single().needs.isEmpty())
        assertNull(repo.requests.single().follow_up)
    }

    @Test fun defaultScheduleDateAndTimeUseCurrentVietnamTime() = vmTest {
        val clock = Clock.fixed(Instant.parse("2026-11-02T18:45:39Z"), ZoneOffset.UTC)
        val vm = create(FakeRepository(), clock)
        assertEquals(LocalDate.of(2026, 11, 3), vm.currentLocalDate())
        assertEquals(LocalTime.of(1, 45), vm.currentLocalTime())
    }

    @Test fun noteIsLimitedToFiveHundredCharacters() = vmTest {
        val vm = create(FakeRepository()); advanceUntilIdle(); vm.setNote("x".repeat(501))
        assertEquals(500, vm.state.value.note.length)
    }

    @Test fun pastScheduleIsBlockedWithoutNetworkCall() = vmTest {
        val repo = FakeRepository(); val vm = create(repo, Clock.fixed(Instant.parse("2026-11-02T18:00:00Z"), ZoneOffset.UTC)); advanceUntilIdle()
        vm.setDate(LocalDate.of(2026, 11, 3)); vm.setTime(LocalTime.of(0, 30)); vm.save()
        assertEquals("past_schedule", vm.state.value.formError); assertTrue(repo.requests.isEmpty())
    }

    @Test fun doubleTapCreatesOnlyOneRequest() = vmTest {
        val gate = CompletableDeferred<RecordInteractionResult>()
        val repo = FakeRepository().apply { action = { gate.await() } }; val vm = create(repo); advanceUntilIdle()
        vm.save(); vm.save(); runCurrent()
        assertEquals(1, repo.requests.size); assertTrue(vm.state.value.isSaving)
        gate.complete(RecordInteractionResult.Success(null, null, emptyList())); advanceUntilIdle(); assertTrue(vm.state.value.saved)
    }

    @Test fun validationErrorsAreKeptByFieldIncludingFollowUpRemindAt() = vmTest {
        val repo = FakeRepository().apply { action = { RecordInteractionResult.ValidationError(mapOf("follow_up.remind_at" to listOf("invalid"))) } }
        val vm = create(repo); advanceUntilIdle(); vm.setDate(LocalDate.of(2026, 11, 4)); vm.setTime(LocalTime.of(9, 0)); vm.save(); advanceUntilIdle()
        assertEquals(listOf("invalid"), vm.state.value.validationErrors["follow_up.remind_at"])
    }

    @Test fun vietnamTimeConvertsToUtcAcrossMidnight() = vmTest {
        val repo = FakeRepository(); val vm = create(repo); advanceUntilIdle()
        vm.setDate(LocalDate.of(2026, 11, 3)); vm.setTime(LocalTime.of(0, 30)); vm.save(); advanceUntilIdle()
        assertEquals("2026-11-02T17:30:00+00:00", repo.requests.single().follow_up?.remind_at)
    }

    @Test fun nullFollowUpIsOmittedByRequestJson() {
        val dto = InteractionCreateDto("contacted", emptyList(), follow_up = null)
        val json = kotlinx.serialization.json.Json { explicitNulls = false }.encodeToString(InteractionCreateDto.serializer(), dto)
        assertFalse(json.contains("follow_up"))
        assertFalse(json.contains("contacted_at"))
    }

    private fun TestScope.create(repo: FakeRepository, clock: Clock = Clock.fixed(Instant.parse("2026-11-02T00:00:00Z"), ZoneOffset.UTC)) =
        CallLogViewModel(7, repo, clock).also { ViewModelStore().also { store -> store.put("call-${stores.size}", it); stores += store } }
    private fun vmTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try { block() } finally { stores.forEach(ViewModelStore::clear); stores.clear(); Dispatchers.resetMain() }
    }

    private class FakeRepository : CallLogRepository {
        val requests = mutableListOf<InteractionCreateDto>()
        var action: suspend () -> RecordInteractionResult = { RecordInteractionResult.Success(null, null, emptyList()) }
        override suspend fun getCallLogCustomer(id: Long) = CustomerDetailResult.Success(customer)
        override suspend fun recordInteraction(id: Long, request: InteractionCreateDto): RecordInteractionResult { requests += request; return action() }
    }
    companion object {
        private val customer = CustomerProfile(7, "Công ty", "Lan", "0900000000", null, null, "medium", null, "active")
    }
}
