package com.example.savvyclub

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.savvyclub.data.loadPuzzlesFromAssets
import com.example.savvyclub.ui.screen.PuzzleScreen
import com.example.savvyclub.ui.theme.SavvyClubTheme
import com.example.savvyclub.viewmodel.SavvyClubViewModel
import com.example.savvyclub.viewmodel.SavvyClubViewModelFactory

@Composable
fun SavvyClubApp(context: Context) {
    val puzzles = loadPuzzlesFromAssets(context)

    val viewModel: SavvyClubViewModel = viewModel(
        factory = SavvyClubViewModelFactory(context, puzzles)
    )

    SavvyClubTheme {
        PuzzleScreen(viewModel = viewModel)
    }
}

