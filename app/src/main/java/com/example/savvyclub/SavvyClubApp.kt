package com.example.savvyclub

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.savvyclub.ui.screen.ComradesScreen
import com.example.savvyclub.ui.screen.ProfileSettingsScreen
import com.example.savvyclub.ui.screen.PuzzleScreen
import com.example.savvyclub.ui.theme.SavvyClubTheme
import com.example.savvyclub.viewmodel.AuthViewModel
import com.example.savvyclub.viewmodel.ComradesViewModel
import com.example.savvyclub.viewmodel.SavvyClubViewModel
import com.example.savvyclub.viewmodel.SavvyClubViewModelFactory

@Composable
fun SavvyClubApp(application: Application) {
    val puzzleViewModel: SavvyClubViewModel = viewModel(
        factory = SavvyClubViewModelFactory(application)
    )
    val authViewModel: AuthViewModel = viewModel()

    val navController = rememberNavController() // 👈 создаём контроллер

    SavvyClubTheme {
        NavHost(navController = navController, startDestination = "puzzles") {
            composable("puzzles") {
                PuzzleScreen(
                    viewModel = puzzleViewModel,
                    authViewModel = authViewModel,
                    navController = navController // 👈 передаём
                )
            }

            composable("comrades") {
                // создаём viewModel для экрана
                val comradesViewModel: ComradesViewModel = viewModel()

                // передаём и viewModel, и onBack
                ComradesScreen(
                    viewModel = comradesViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("profile_settings") {
                // передай AuthViewModel, чтобы можно было редактировать имя/аватар
                ProfileSettingsScreen(
                    authViewModel = authViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}


