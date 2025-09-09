package com.example.savvyclub

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.savvyclub.ui.screen.PuzzleScreen
import com.example.savvyclub.ui.theme.SavvyClubTheme
import com.example.savvyclub.viewmodel.SavvyClubViewModel
import com.example.savvyclub.viewmodel.SavvyClubViewModelFactory

@Composable
fun SavvyClubApp(application: Application) {
    val viewModel: SavvyClubViewModel = viewModel(
        factory = SavvyClubViewModelFactory(application)
    )

    SavvyClubTheme {
        PuzzleScreen(viewModel)
    }
}
