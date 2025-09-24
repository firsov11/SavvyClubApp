package com.example.savvyclub

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.savvyclub.data.UserPreferences
import com.example.savvyclub.ui.screen.ComradesScreen
import com.example.savvyclub.ui.screen.OpeningRemarksScreen
import com.example.savvyclub.ui.screen.ProfileSettingsScreen
import com.example.savvyclub.ui.screen.PuzzleScreen
import com.example.savvyclub.ui.theme.SavvyClubTheme
import com.example.savvyclub.viewmodel.AuthViewModel
import com.example.savvyclub.viewmodel.AuthViewModelFactory
import com.example.savvyclub.viewmodel.ComradesViewModel
import com.example.savvyclub.viewmodel.SavvyClubViewModel
import com.example.savvyclub.viewmodel.SavvyClubViewModelFactory

@Composable
fun SavvyClubApp(application: Application) {
    val puzzleViewModel: SavvyClubViewModel = viewModel(
        factory = SavvyClubViewModelFactory(application)
    )

    // Создаём UserPreferences или другой объект, который нужен AuthViewModel
    val userPreferences = remember { UserPreferences(application) }

    // Используем фабрику для AuthViewModel
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(userPreferences)
    )

    val navController = rememberNavController()

    SavvyClubTheme {
        NavHost(navController = navController, startDestination = "puzzles") {
            composable("puzzles") {
                PuzzleScreen(
                    viewModel = puzzleViewModel,
                    authViewModel = authViewModel,
                    navController = navController
                )
            }

            composable("comrades") {
                val comradesViewModel: ComradesViewModel = viewModel()
                ComradesScreen(
                    viewModel = comradesViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("profile_settings") {
                ProfileSettingsScreen(
                    authViewModel = authViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("opening_remarks") {
                OpeningRemarksScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}



