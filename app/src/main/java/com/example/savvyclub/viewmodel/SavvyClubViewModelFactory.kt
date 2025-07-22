package com.example.savvyclub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import android.content.Context
import com.example.savvyclub.model.Puzzle

class SavvyClubViewModelFactory(
    private val context: Context,
    private val puzzles: List<Puzzle>
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SavvyClubViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SavvyClubViewModel(context, puzzles) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
