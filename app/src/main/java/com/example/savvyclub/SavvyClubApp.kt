package com.example.savvyclub.ui

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.savvyclub.ui.screen.PuzzleScreen
import com.example.savvyclub.ui.theme.SavvyClubTheme
import com.example.savvyclub.viewmodel.AuthViewModel
import com.example.savvyclub.viewmodel.SavvyClubViewModel
import com.example.savvyclub.viewmodel.SavvyClubViewModelFactory

@Composable
fun SavvyClubApp(application: Application) {
    // 🔹 ViewModel для головоломок
    val puzzleViewModel: SavvyClubViewModel = viewModel(
        factory = SavvyClubViewModelFactory(application)
    )

    // 🔹 ViewModel для авторизации
    val authViewModel: AuthViewModel = viewModel()

    SavvyClubTheme {
        PuzzleScreen(
            viewModel = puzzleViewModel,
            authViewModel = authViewModel
        )
    }
}

