package com.example.savvyclub

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.savvyclub.data.UserPreferences
import com.example.savvyclub.ui.screen.*
import com.example.savvyclub.ui.theme.SavvyClubTheme
import com.example.savvyclub.viewmodel.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SavvyClubApp(application: Application) {
    val puzzleViewModel: SavvyClubViewModel = viewModel(factory = SavvyClubViewModelFactory(application))
    val userPreferences = remember { UserPreferences(application) }
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(userPreferences))

    val overlayScreen = remember { mutableStateOf<String?>(null) }

    SavvyClubTheme {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {

            // ---------------- PuzzleScreen всегда в фоне ----------------
            PuzzleScreen(
                viewModel = puzzleViewModel,
                authViewModel = authViewModel,
                overlayScreen = overlayScreen
            )

            // ---------------- Перехват системной кнопки назад ----------------
            BackHandler(enabled = overlayScreen.value != null) {
                overlayScreen.value = null
            }

            // ---------------- Overlay экраны ----------------
            AnimatedContent(
                targetState = overlayScreen.value,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) with fadeOut(animationSpec = tween(300))
                }
            ) { screen ->
                when (screen) {
                    "comrades" -> {
                        val comradesViewModel: ComradesViewModel = viewModel()
                        ComradesScreen(
                            viewModel = comradesViewModel,
                            onBack = { overlayScreen.value = null }
                        )
                    }
                    "profile_settings" -> {
                        ProfileSettingsScreen(
                            authViewModel = authViewModel,
                            onBack = { overlayScreen.value = null }
                        )
                    }
                    "opening_remarks" -> {
                        OpeningRemarksScreen(
                            onBack = { overlayScreen.value = null }
                        )
                    }
                }
            }
        }
    }
}

