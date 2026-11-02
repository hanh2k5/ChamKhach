package com.telesales.assistant.ui.customers

import androidx.lifecycle.ViewModelStore
import com.telesales.assistant.data.repository.CustomerLoadResult
import com.telesales.assistant.data.repository.CustomerPage
import com.telesales.assistant.data.repository.CustomerQuery
import com.telesales.assistant.data.repository.CustomerRepository
import com.telesales.assistant.domain.model.Customer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CustomersViewModelTest {
    private val stores = mutableListOf<ViewModelStore>()

    @Test fun loadsContentAndEmptyStates() = vmTest {
        val repo = FakeRepository { _, page -> success(page, if (page == 1) listOf(customer(1)) else emptyList()) }
        val vm = create(repo)
        assertTrue(vm.state.value.isLoading)
        advanceUntilIdle()
        assertEquals(1, vm.state.value.customers.size)

        val emptyRepo = FakeRepository { _, _ -> success(1, emptyList()) }
        val emptyVm = create(emptyRepo)
        advanceUntilIdle()
        assertTrue(emptyVm.state.value.customers.isEmpty())
        assertFalse(emptyVm.state.value.isLoading)
    }

    @Test fun reportsErrorAndRetryLoadsAgain() = vmTest {
        var fail = true
        val repo = FakeRepository { _, page -> if (fail) CustomerLoadResult.NetworkError else success(page, listOf(customer(2))) }
        val vm = create(repo)
        advanceUntilIdle()
        assertEquals(CustomerError.Network, vm.state.value.error)
        fail = false
        vm.retry()
        advanceUntilIdle()
        assertEquals(2L, vm.state.value.customers.single().id)
    }

    @Test fun debouncesSearchAndOnlyLatestQueryWins() = vmTest {
        val repo = FakeRepository { query, page ->
            if (query.text == "A") delay(1_000)
            if (query.text == "B") delay(10)
            success(page, listOf(customer(if (query.text == "B") 20 else 10)))
        }
        val vm = create(repo)
        advanceUntilIdle()
        vm.onQueryChanged("A")
        advanceTimeBy(300)
        runCurrent()
        vm.onQueryChanged("B")
        advanceTimeBy(300)
        runCurrent()
        advanceUntilIdle()
        assertEquals(listOf(20L), vm.state.value.customers.map(Customer::id))
        assertEquals("B", repo.calls.last().first.text)
    }

    @Test fun loadingMoreTwiceStartsOnlyOneRequestAndUsesNextPage() = vmTest {
        val repo = FakeRepository { _, page -> success(page, listOf(customer(page.toLong())), lastPage = 2) }
        val vm = create(repo)
        advanceUntilIdle()
        vm.loadMore()
        vm.loadMore()
        assertTrue(vm.state.value.isLoadingMore)
        advanceUntilIdle()
        assertEquals(2, repo.calls.size)
        assertEquals(2, repo.calls.last().second)
        assertEquals(listOf(1L, 2L), vm.state.value.customers.map(Customer::id))
        assertFalse(vm.state.value.hasMore)
        vm.loadMore()
        advanceUntilIdle()
        assertEquals(2, repo.calls.size)
    }

    @Test fun changingFilterResetsPageAndClearsOldRows() = vmTest {
        val repo = FakeRepository { query, page -> success(page, listOf(customer(if (query.status == "archived") 9 else page.toLong())), 2) }
        val vm = create(repo)
        advanceUntilIdle()
        vm.loadMore(); advanceUntilIdle()
        vm.onStatusChanged("archived")
        assertTrue(vm.state.value.customers.isEmpty())
        advanceUntilIdle()
        assertEquals("archived", repo.calls.last().first.status)
        assertEquals(1, repo.calls.last().second)
        assertEquals(listOf(9L), vm.state.value.customers.map(Customer::id))
    }

    private fun TestScope.create(repo: CustomerRepository): CustomersViewModel = CustomersViewModel(repo).also {
        ViewModelStore().also { store -> store.put("customers-${stores.size}", it); stores += store }
    }

    private fun vmTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try { block() } finally { stores.forEach(ViewModelStore::clear); stores.clear(); Dispatchers.resetMain() }
    }

    private class FakeRepository(private val action: suspend (CustomerQuery, Int) -> CustomerLoadResult) : CustomerRepository {
        val calls = mutableListOf<Pair<CustomerQuery, Int>>()
        override suspend fun getCustomers(query: CustomerQuery, page: Int): CustomerLoadResult {
            calls += query to page
            return action(query, page)
        }
    }

    private fun success(page: Int, customers: List<Customer>, lastPage: Int = 1) =
        CustomerLoadResult.Success(CustomerPage(customers, page, lastPage))

    private fun customer(id: Long) = Customer(id, "Shop $id", null, "0900000000", null, "medium", "active", 0, null, null)
}
