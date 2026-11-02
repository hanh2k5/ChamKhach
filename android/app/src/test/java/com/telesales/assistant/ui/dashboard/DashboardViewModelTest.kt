package com.telesales.assistant.ui.dashboard

import androidx.lifecycle.ViewModelStore
import com.telesales.assistant.data.repository.DashboardRepository
import com.telesales.assistant.data.repository.DashboardResult
import com.telesales.assistant.domain.model.DashboardSummary
import com.telesales.assistant.domain.model.FollowUp
import com.telesales.assistant.domain.model.FollowUpCustomer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    private val stores = mutableListOf<ViewModelStore>()

    @Test fun loadingThenContentUsesAllServerCountsAndItemOrder() = vmTest {
        val pending = CompletableDeferred<DashboardResult>()
        val vm = create(FakeRepository { pending.await() })
        assertTrue(vm.state.value.isLoading)
        pending.complete(DashboardResult.Success(summary(7, 4, 12, listOf(item(1, "overdue"), item(2, "today")))))
        advanceUntilIdle()
        assertEquals(7, vm.state.value.dashboard?.overdueCount)
        assertEquals(4, vm.state.value.dashboard?.todayCount)
        assertEquals(12, vm.state.value.dashboard?.potentialCount)
        assertEquals(listOf(1L, 2L), vm.state.value.dashboard?.items?.map(FollowUp::id))
        assertFalse(vm.state.value.isLoading)
    }

    @Test fun emptyItemsStillKeepTheThreeServerCounters() = vmTest {
        val vm = create(FakeRepository { DashboardResult.Success(summary(2, 0, 9, emptyList())) })
        advanceUntilIdle()
        assertNotNull(vm.state.value.dashboard)
        assertTrue(vm.state.value.dashboard!!.items.isEmpty())
        assertEquals(2, vm.state.value.dashboard!!.overdueCount)
        assertEquals(0, vm.state.value.dashboard!!.todayCount)
        assertEquals(9, vm.state.value.dashboard!!.potentialCount)
    }

    @Test fun errorCanBeRetriedAndRefreshUpdatesCounts() = vmTest {
        var result: DashboardResult = DashboardResult.NetworkError
        val repository = FakeRepository { result }
        val vm = create(repository); advanceUntilIdle()
        assertEquals(DashboardError.Network, vm.state.value.error)
        result = DashboardResult.Success(summary(3, 5, 8, emptyList()))
        vm.retry(); advanceUntilIdle()
        assertNull(vm.state.value.error)
        assertEquals(3, vm.state.value.dashboard?.overdueCount)
        result = DashboardResult.Success(summary(1, 6, 10, emptyList()))
        vm.refresh(); advanceUntilIdle()
        assertEquals(1, vm.state.value.dashboard?.overdueCount)
        assertEquals(6, vm.state.value.dashboard?.todayCount)
        assertEquals(10, vm.state.value.dashboard?.potentialCount)
    }

    @Test fun missingConfigurationShowsSetupError() = vmTest {
        val repository = FakeRepository { DashboardResult.SetupRequired }
        val vm = create(repository); advanceUntilIdle()
        assertEquals(DashboardError.SetupRequired, vm.state.value.error)
        assertNull(vm.state.value.dashboard)
    }

    private fun TestScope.create(repository: DashboardRepository) = DashboardViewModel(
        repository, Clock.fixed(Instant.parse("2026-11-02T00:00:00Z"), ZoneOffset.UTC),
    ).also { ViewModelStore().also { store -> store.put("dashboard-${stores.size}", it); stores += store } }

    private fun vmTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try { block() } finally { stores.forEach(ViewModelStore::clear); stores.clear(); Dispatchers.resetMain() }
    }

    private class FakeRepository(private val action: suspend () -> DashboardResult) : DashboardRepository {
        var calls = 0
        override suspend fun getDashboard(): DashboardResult { calls++; return action() }
    }

    companion object {
        private fun summary(overdue: Int, today: Int, potential: Int, items: List<FollowUp>) = DashboardSummary(overdue, today, potential, items)
        private fun item(id: Long, group: String) = FollowUp(
            id, Instant.parse("2026-11-02T01:00:00Z"), "pending", group,
            FollowUpCustomer(10, "Công ty", null, "0900000000"), null,
        )
    }
}
