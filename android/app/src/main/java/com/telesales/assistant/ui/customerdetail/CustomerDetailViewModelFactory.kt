package com.telesales.assistant.ui.customerdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.telesales.assistant.data.repository.CustomerDetailRepository
import java.time.Clock
import java.time.ZoneId

class CustomerDetailViewModelFactory(
    private val customerId: Long,
    private val repository: CustomerDetailRepository,
    private val clock: Clock,
    private val zoneId: ZoneId,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(CustomerDetailViewModel::class.java))
        return modelClass.cast(CustomerDetailViewModel(customerId, repository, clock, zoneId))
    }
}
