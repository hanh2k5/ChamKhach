package com.telesales.assistant.ui.customerdetail

import androidx.lifecycle.ViewModelStore
import com.telesales.assistant.data.repository.DeleteCustomerResult
import com.telesales.assistant.data.repository.CustomerDetailFailure
import com.telesales.assistant.data.repository.CustomerDetailRepository
import com.telesales.assistant.data.repository.CustomerDetailResult
import com.telesales.assistant.data.repository.InteractionPage
import com.telesales.assistant.data.repository.InteractionPageResult
import com.telesales.assistant.domain.model.CustomerProfile
import com.telesales.assistant.domain.model.Interaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerDetailViewModelTest {
    private val stores = mutableListOf<ViewModelStore>()

    @Test fun loadsContentAndTimelineAndSupportsRetryAfterDetailError() = vmTest {
        var fail = true
        val repo = FakeRepository().apply {
            detailAction = { if (fail) CustomerDetailResult.Failure(CustomerDetailFailure.Network) else CustomerDetailResult.Success(customer) }
        }
        val vm = create(repo)
        assertTrue(vm.state.value.isLoading)
        advanceUntilIdle()
        assertEquals(CustomerDetailFailure.Network, vm.state.value.error)
        fail = false
        vm.retry()
        advanceUntilIdle()
        assertEquals(customer, vm.state.value.customer)
        assertFalse(vm.state.value.isTimelineLoading)
        assertTrue(vm.state.value.interactions.isEmpty())
    }

    @Test fun loadsTimelinePagesAndIgnoresRepeatedLoadMoreWhileInFlight() = vmTest {
        val pending = CompletableDeferred<InteractionPageResult>()
        val repo = FakeRepository().apply {
            interactionsAction = { page -> if (page == 2) pending.await() else success(page, listOf(interaction(page.toLong())), 3) }
        }
        val vm = create(repo)
        advanceUntilIdle()
        vm.loadMore()
        vm.loadMore()
        assertTrue(vm.state.value.isLoadingMore)
        runCurrent()
        assertEquals(listOf(1, 2), repo.pages)
        pending.complete(success(2, listOf(interaction(2)), 3))
        advanceUntilIdle()
        assertEquals(listOf(1L, 2L), vm.state.value.interactions.map(Interaction::id))
        vm.loadMore()
        advanceUntilIdle()
        assertEquals(listOf(1, 2, 3), repo.pages)
        assertFalse(vm.state.value.hasMore)
        vm.loadMore()
        advanceUntilIdle()
        assertEquals(listOf(1, 2, 3), repo.pages)
    }

    @Test fun keepsNoCallsAsEmptyTimelineAndDeletesWithFollowUpIds() = vmTest {
        val repo = FakeRepository().apply {
            deleteAction = { DeleteCustomerResult.Success(listOf(4, 5)) }
        }
        val vm = create(repo)
        advanceUntilIdle()
        assertTrue(vm.state.value.interactions.isEmpty())
        vm.showDeleteConfirmation()
        assertTrue(vm.state.value.deleteConfirmationVisible)
        vm.deleteCustomer()
        advanceUntilIdle()
        assertTrue(vm.state.value.isDeleted)
        assertEquals(listOf(4L, 5L), vm.state.value.deletedFollowUpIds)
        assertFalse(vm.state.value.deleteConfirmationVisible)
    }

    @Test fun reportsDeleteFailureAndCanRetry() = vmTest {
        var fail = true
        val repo = FakeRepository().apply {
            deleteAction = { if (fail) DeleteCustomerResult.Failure(CustomerDetailFailure.Server) else DeleteCustomerResult.Success(emptyList()) }
        }
        val vm = create(repo)
        advanceUntilIdle()
        vm.deleteCustomer()
        advanceUntilIdle()
        assertEquals(CustomerDetailFailure.Server, vm.state.value.deleteError)
        fail = false
        vm.retryDelete()
        advanceUntilIdle()
        assertTrue(vm.state.value.isDeleted)
    }

    private fun TestScope.create(repo: FakeRepository) = CustomerDetailViewModel(7, repo).also {
        ViewModelStore().also { store -> store.put("detail-${stores.size}", it); stores += store }
    }

    private fun vmTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try { block() } finally { stores.forEach(ViewModelStore::clear); stores.clear(); Dispatchers.resetMain() }
    }

    private class FakeRepository : CustomerDetailRepository {
        var detailAction: suspend () -> CustomerDetailResult = { CustomerDetailResult.Success(customer) }
        var interactionsAction: suspend (Int) -> InteractionPageResult = { page -> success(page, emptyList()) }
        var deleteAction: suspend () -> DeleteCustomerResult = { DeleteCustomerResult.Success(emptyList()) }
        val pages = mutableListOf<Int>()
        override suspend fun getCustomerDetail(id: Long) = detailAction()
        override suspend fun getInteractions(id: Long, page: Int): InteractionPageResult { pages += page; return interactionsAction(page) }
        override suspend fun deleteCustomer(id: Long) = deleteAction()
    }

    companion object {
        private val customer = CustomerProfile(7, "Công ty", "Lan", "0900000000", null, null, null, "medium", "active")
        private fun success(page: Int, items: List<Interaction>, lastPage: Int = page) =
            InteractionPageResult.Success(InteractionPage(items, page, lastPage))
        private fun interaction(id: Long) = Interaction(id, "interested", listOf("website"), null, java.time.Instant.parse("2026-11-02T00:00:00Z"))
    }
}
