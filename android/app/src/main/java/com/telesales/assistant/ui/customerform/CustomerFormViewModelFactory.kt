package com.telesales.assistant.ui.customerform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.telesales.assistant.data.repository.CustomerFormRepository
import com.telesales.assistant.reminder.ReminderCoordinator
import java.time.Clock

class CustomerFormViewModelFactory(
    private val repository: CustomerFormRepository,
    private val customerId: Long?,
    private val clock: Clock,
    private val reminderCoordinator: ReminderCoordinator?,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(CustomerFormViewModel::class.java))
        return modelClass.cast(CustomerFormViewModel(repository, customerId, clock, reminderCoordinator))
    }
}
