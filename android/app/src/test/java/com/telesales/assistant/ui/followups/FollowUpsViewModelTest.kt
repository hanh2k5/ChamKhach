package com.telesales.assistant.ui.followups

import androidx.lifecycle.ViewModelStore
import com.telesales.assistant.data.repository.FollowUpActionResult
import com.telesales.assistant.data.repository.FollowUpListResult
import com.telesales.assistant.data.repository.FollowUpPage
import com.telesales.assistant.data.repository.FollowUpRepository
import com.telesales.assistant.domain.model.FollowUp
import com.telesales.assistant.domain.model.FollowUpCustomer
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
class FollowUpsViewModelTest {
    private val stores = mutableListOf<ViewModelStore>()

    @Test fun loadsContentEmptyAndCanRetryAfterError() = vmTest {
        val repo = FakeRepository().apply { listAction = { _, _ -> FollowUpListResult.NetworkError } }
        val vm = create(repo)
        assertTrue(vm.state.value.isLoading)
        advanceUntilIdle()
        assertEquals(FollowUpsError.Network, vm.state.value.error)
        repo.listAction = { page, _ -> success(page, emptyList()) }
        vm.retry(); advanceUntilIdle()
        assertNull(vm.state.value.error)
        assertTrue(vm.state.value.items.isEmpty())
        assertFalse(vm.state.value.isLoading)
    }

    @Test fun sortsUsingServerGroupOrderThenReminderTime() = vmTest {
        val unordered = listOf(item(4, "upcoming", "2026-11-03T02:00:00Z"), item(2, "today", "2026-11-02T04:00:00Z"), item(3, "today", "2026-11-02T02:00:00Z"), item(1, "overdue", "2026-11-01T20:00:00Z"))
        val vm = create(FakeRepository().apply { listAction = { page, _ -> success(page, unordered) } }); advanceUntilIdle()
        assertEquals(listOf(1L, 3L, 2L, 4L), vm.state.value.items.map(FollowUp::id))
    }

    @Test fun loadsSelectedPaginationPage() = vmTest {
        val repo = FakeRepository().apply {
            listAction = { page, _ ->
                if (page == 1) success(1, listOf(item(1, "today")), 2)
                else success(2, listOf(item(2, "upcoming")), 2)
            }
        }
        val vm = create(repo); advanceUntilIdle()
        vm.goToPage(2); advanceUntilIdle()
        assertEquals(listOf(1, 2), repo.requestedPages)
        assertEquals(listOf(2L), vm.state.value.items.map(FollowUp::id))
        assertEquals(2, vm.state.value.currentPage)
        assertEquals(2, vm.state.value.lastPage)
    }

    @Test fun completeRemovesItemFromPendingList() = vmTest {
        val repo = FakeRepository(); val vm = create(repo); advanceUntilIdle()
        vm.complete(item(1, "today")); advanceUntilIdle()
        assertTrue(vm.state.value.items.isEmpty())
        assertEquals(listOf(1L to "complete"), repo.actions.map { it.first to it.second })
    }

    @Test fun validRescheduleUpdatesItemAndPastScheduleIsBlocked() = vmTest {
        val repo = FakeRepository().apply { actionAction = { _, action, at ->
            if (action == "reschedule") FollowUpActionResult.Success(firstItem.copy(remindAt = Instant.parse(at), group = "upcoming"))
            else FollowUpActionResult.Success(firstItem)
        } }
        val vm = create(repo); advanceUntilIdle()
        vm.beginReschedule(repo.firstItem); vm.setRescheduleDate(LocalDate.of(2026, 11, 3)); vm.setRescheduleTime(LocalTime.of(9, 30)); vm.submitReschedule(); advanceUntilIdle()
        assertEquals("2026-11-03T02:30:00+00:00", repo.actions.single().third)
        assertEquals("2026-11-03T02:30:00Z", vm.state.value.items.single().remindAt.toString())
        assertEquals("upcoming", vm.state.value.items.single().group)
        val before = repo.actions.size
        vm.beginReschedule(vm.state.value.items.single()); vm.setRescheduleDate(LocalDate.of(2026, 11, 2)); vm.setRescheduleTime(LocalTime.of(6, 0)); vm.submitReschedule()
        assertEquals("past", vm.state.value.rescheduleError)
        assertEquals(before, repo.actions.size)
    }

    @Test fun conflictShowsNoticeAndRefreshesTheList() = vmTest {
        var calls = 0
        val repo = FakeRepository().apply {
            listAction = { page, _ -> calls++; success(page, listOf(item(1, "today"))) }
            actionAction = { _, _, _ -> FollowUpActionResult.NotPending }
        }
        val vm = create(repo); advanceUntilIdle(); vm.complete(repo.firstItem); advanceUntilIdle()
        assertEquals("conflict", vm.state.value.notice)
        assertEquals(2, calls)
        assertEquals(listOf(1L), vm.state.value.items.map(FollowUp::id))
    }

    @Test fun repeatedActionForBusyItemCreatesOnlyOneRequest() = vmTest {
        val gate = CompletableDeferred<FollowUpActionResult>()
        val repo = FakeRepository().apply { actionAction = { _, _, _ -> gate.await() } }
        val vm = create(repo); advanceUntilIdle(); vm.complete(repo.firstItem); vm.complete(repo.firstItem); runCurrent()
        assertEquals(1, repo.actions.size)
        assertEquals(setOf(1L), vm.state.value.busyIds)
        gate.complete(FollowUpActionResult.Success(repo.firstItem.copy(status = "completed"))); advanceUntilIdle()
        assertTrue(vm.state.value.items.isEmpty())
    }

    private fun TestScope.create(repo: FakeRepository) = FollowUpsViewModel(repo, fixedClock).also {
        ViewModelStore().also { store -> store.put("follow-ups-${stores.size}", it); stores += store }
    }

    private fun vmTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try { block() } finally { stores.forEach(ViewModelStore::clear); stores.clear(); Dispatchers.resetMain() }
    }

    private class FakeRepository : FollowUpRepository {
        var listAction: suspend (Int, FakeRepository) -> FollowUpListResult = { page, _ -> success(page, listOf(firstItem)) }
        var actionAction: suspend (Long, String, String?) -> FollowUpActionResult = { _, _, _ -> FollowUpActionResult.Success(firstItem) }
        val requestedPages = mutableListOf<Int>()
        val actions = mutableListOf<Triple<Long, String, String?>>()
        val firstItem get() = item(1, "today")
        override suspend fun getFollowUps(page: Int): FollowUpListResult { requestedPages += page; return listAction(page, this) }
        override suspend fun applyAction(id: Long, action: String, remindAt: String?): FollowUpActionResult {
            actions += Triple(id, action, remindAt); return actionAction(id, action, remindAt)
        }
    }

    companion object {
        private val fixedClock: Clock = Clock.fixed(Instant.parse("2026-11-01T23:00:00Z"), ZoneOffset.UTC)
        private fun success(page: Int, items: List<FollowUp>, lastPage: Int = page) = FollowUpListResult.Success(FollowUpPage(items, page, lastPage))
        private fun item(id: Long, group: String, at: String = "2026-11-02T01:00:00Z") = FollowUp(
            id, Instant.parse(at), "pending", group, FollowUpCustomer(7, "Công ty", "Lan", "0900000000"), null,
        )
    }
}
