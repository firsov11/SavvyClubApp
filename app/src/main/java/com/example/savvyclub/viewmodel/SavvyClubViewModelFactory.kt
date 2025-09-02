package com.example.savvyclub.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SavvyClubViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SavvyClubViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SavvyClubViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
