package com.telesales.assistant.di

import android.content.Context
import com.telesales.assistant.data.network.ApiServiceFactory
import com.telesales.assistant.data.network.ApiServiceProvider
import com.telesales.assistant.data.settings.SettingsStore
import com.telesales.assistant.data.repository.ApiCustomerRepository
import com.telesales.assistant.data.repository.CustomerRepository
import com.telesales.assistant.data.repository.CustomerFormRepository
import com.telesales.assistant.data.repository.CustomerDetailRepository

/**
 * Interface cho Dependency Container theo mô hình Manual DI (SRS mục 2 & 9.1).
 * Các dependency cụ thể (SettingsRepository, CustomerRepository, Database, ApiService)
 * sẽ được cung cấp qua container này ở các task A1..A8.
 */
interface AppContainer {
    val settingsStore: SettingsStore
    val apiServiceFactory: ApiServiceProvider
    val customerRepository: CustomerRepository
    val customerFormRepository: CustomerFormRepository
    val customerDetailRepository: CustomerDetailRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
    override val settingsStore = SettingsStore(context)
    override val apiServiceFactory = ApiServiceFactory()
    private val customerApiRepository = ApiCustomerRepository(settingsStore, apiServiceFactory)
    override val customerRepository: CustomerRepository = customerApiRepository
    override val customerFormRepository: CustomerFormRepository = customerApiRepository
    override val customerDetailRepository: CustomerDetailRepository = customerApiRepository
}

