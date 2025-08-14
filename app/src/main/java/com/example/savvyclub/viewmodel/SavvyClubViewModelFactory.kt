package com.example.savvyclub.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SavvyClubViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SavvyClubViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SavvyClubViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}




