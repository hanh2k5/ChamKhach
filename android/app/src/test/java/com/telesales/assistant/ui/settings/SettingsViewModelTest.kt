package com.telesales.assistant.ui.settings

import androidx.lifecycle.ViewModelStore
import com.telesales.assistant.data.network.ApiService
import com.telesales.assistant.data.network.ApiServiceProvider
import com.telesales.assistant.data.network.PingDto
import com.telesales.assistant.data.network.CustomerPageDto
import com.telesales.assistant.data.network.CustomerProfileResponseDto
import com.telesales.assistant.data.network.CustomerProfileDto
import com.telesales.assistant.data.network.CustomerCreateDto
import com.telesales.assistant.data.network.CustomerUpdateDto
import com.telesales.assistant.data.network.InteractionPageDto
import com.telesales.assistant.data.network.CustomerStatusDto
import com.telesales.assistant.data.settings.InMemoryPreferencesDataStore
import com.telesales.assistant.data.settings.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val viewModelStores = mutableListOf<ViewModelStore>()

    @Test
    fun invalidUrlShowsFormErrorWithoutCreatingApiService() = runViewModelTest {
        val apiProvider = FakeApiServiceProvider()
        val viewModel = createViewModel(apiProvider)
        advanceUntilIdle()

        viewModel.onBaseUrlChanged("localhost:8000/api/v1")
        viewModel.onTokenChanged("token")
        viewModel.checkConnection()
        advanceUntilIdle()

        assertEquals(SettingsStatus.InvalidUrl, viewModel.state.value.status)
        assertTrue(viewModel.state.value.baseUrlInvalid)
        assertEquals(0, apiProvider.createCount)
    }

    @Test
    fun successfulPingShowsConnected() = runViewModelTest {
        val apiProvider = FakeApiServiceProvider()
        val viewModel = createViewModel(apiProvider)
        advanceUntilIdle()
        viewModel.onBaseUrlChanged(" http://10.0.2.2:8000/api/v1 ")
        viewModel.onTokenChanged(" token ")

        viewModel.checkConnection()
        advanceUntilIdle()

        assertEquals(SettingsStatus.Connected, viewModel.state.value.status)
        assertEquals("http://10.0.2.2:8000/api/v1/", apiProvider.lastBaseUrl)
        assertEquals("token", apiProvider.lastToken)
    }

    @Test
    fun unauthorizedResponseSuggestsCheckingToken() = runViewModelTest {
        val apiProvider = FakeApiServiceProvider {
            throw HttpException(Response.error<PingDto>(401, "{}".toResponseBody("application/json".toMediaType())))
        }
        val viewModel = createViewModel(apiProvider)
        advanceUntilIdle()
        viewModel.onBaseUrlChanged("https://api.example.test/api/v1/")
        viewModel.onTokenChanged("token")

        viewModel.checkConnection()
        advanceUntilIdle()

        assertEquals(SettingsStatus.Unauthorized, viewModel.state.value.status)
        assertFalse(viewModel.state.value.isChecking)
    }

    @Test
    fun networkFailureShowsRetryableNetworkStatus() = runViewModelTest {
        val apiProvider = FakeApiServiceProvider { throw IOException("offline") }
        val viewModel = createViewModel(apiProvider)
        advanceUntilIdle()
        viewModel.onBaseUrlChanged("https://api.example.test/api/v1/")
        viewModel.onTokenChanged("token")

        viewModel.checkConnection()
        advanceUntilIdle()

        assertEquals(SettingsStatus.NetworkError, viewModel.state.value.status)
        assertFalse(viewModel.state.value.isChecking)
    }

    private fun TestScope.createViewModel(apiProvider: FakeApiServiceProvider): SettingsViewModel {
        val viewModel = SettingsViewModel(
            SettingsStore(InMemoryPreferencesDataStore()),
            apiProvider,
        )
        ViewModelStore().also { store ->
            store.put("settings", viewModel)
            viewModelStores += store
        }
        return viewModel
    }

    private fun runViewModelTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            block()
        } finally {
            viewModelStores.forEach(ViewModelStore::clear)
            viewModelStores.clear()
            Dispatchers.resetMain()
        }
    }

    private class FakeApiServiceProvider(
        private val pingAction: suspend () -> PingDto = { PingDto(status = "ok", message = "pong") },
    ) : ApiServiceProvider {
        var createCount = 0
            private set
        var lastBaseUrl: String? = null
            private set
        var lastToken: String? = null
            private set

        override fun create(baseUrl: String, token: String): ApiService {
            createCount++
            lastBaseUrl = baseUrl
            lastToken = token
            return object : ApiService {
                override suspend fun ping(): PingDto = pingAction()
                override suspend fun customers(query: String?, status: String, potential: String?, lastResult: String?, page: Int, perPage: Int) = CustomerPageDto()
                override suspend fun customer(id: Long) = CustomerProfileResponseDto(CustomerProfileDto(id, phone = "0901234567"))
                override suspend fun createCustomer(customer: CustomerCreateDto) = CustomerProfileResponseDto(CustomerProfileDto(1, phone = customer.phone))
                override suspend fun updateCustomer(id: Long, customer: CustomerUpdateDto) = CustomerProfileResponseDto(CustomerProfileDto(id, phone = customer.phone))
                override suspend fun interactions(id: Long, page: Int, perPage: Int) = InteractionPageDto()
                override suspend fun createInteraction(id: Long, interaction: com.telesales.assistant.data.network.InteractionCreateDto) = com.telesales.assistant.data.network.InteractionCreateResponseDto(com.telesales.assistant.data.network.InteractionDto(1, interaction.result, interaction.needs, interaction.note, "2026-11-02T00:00:00+00:00"))
                override suspend fun followUps(status: String, group: String, page: Int, perPage: Int) = com.telesales.assistant.data.network.FollowUpPageDto()
                override suspend fun updateFollowUp(id: Long, request: com.telesales.assistant.data.network.FollowUpActionDto) = com.telesales.assistant.data.network.FollowUpResponseDto(com.telesales.assistant.data.network.FollowUpDto(id, "2026-11-02T00:00:00+00:00", "pending", "today", com.telesales.assistant.data.network.FollowUpCustomerDto(1, phone = "0900000000")))
                override suspend fun dashboard() = com.telesales.assistant.data.network.DashboardDto(0, 0, 0)
                override suspend fun updateCustomerStatus(id: Long, status: CustomerStatusDto) = CustomerProfileResponseDto(CustomerProfileDto(id, phone = "0901234567", status = status.status))
            }
        }
    }
}
