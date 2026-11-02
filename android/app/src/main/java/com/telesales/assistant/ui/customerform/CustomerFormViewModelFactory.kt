package com.telesales.assistant.ui.customerform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.telesales.assistant.data.repository.CustomerFormRepository

class CustomerFormViewModelFactory(
    private val repository: CustomerFormRepository,
    private val customerId: Long?,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(CustomerFormViewModel::class.java))
        return modelClass.cast(CustomerFormViewModel(repository, customerId))
    }
}
