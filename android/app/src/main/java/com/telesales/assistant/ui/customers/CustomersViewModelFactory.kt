package com.telesales.assistant.ui.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.telesales.assistant.data.repository.CustomerRepository

class CustomersViewModelFactory(
    private val repository: CustomerRepository,
    private val initialPotential: String? = null,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(CustomersViewModel::class.java))
        return modelClass.cast(CustomersViewModel(repository, initialPotential))
    }
}
