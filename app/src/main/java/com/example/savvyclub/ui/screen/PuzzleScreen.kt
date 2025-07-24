package com.example.savvyclub.ui.screen

import android.content.Context
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.savvyclub.R
import com.example.savvyclub.ui.component.PuzzleImageFromAssets
import com.example.savvyclub.viewmodel.SavvyClubViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzleScreen(viewModel: SavvyClubViewModel) {
    val puzzle by viewModel.currentPuzzle.collectAsState()
    val showAnswer by viewModel.showAnswer.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    // Вынесены все stringResource вызовы для использования в @Composable контексте
    val menuTitle = stringResource(R.string.menu_title)
    val aboutText = stringResource(R.string.about)
    val resetText = stringResource(R.string.button_reset)
    val dialogTitle = stringResource(R.string.dialog_title)
    val dialogMessage = stringResource(R.string.dialog_message)
    val dialogConfirm = stringResource(R.string.dialog_confirm)
    val dialogCancel = stringResource(R.string.dialog_cancel)
    val dialogOk = stringResource(R.string.dialog_ok)
    val appName = stringResource(R.string.app_name)
    val noPuzzlesLeft = stringResource(R.string.no_puzzles_left)
    val buttonShowPuzzle = stringResource(R.string.button_show_puzzle)
    val buttonShowAnswer = stringResource(R.string.button_show_answer)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = menuTitle,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )

                HorizontalDivider()

                NavigationDrawerItem(
                    icon = null,
                    label = { Text(aboutText) },
                    selected = false,
                    onClick = {
                        showAboutDialog = true
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    icon = null,
                    label = { Text(resetText) },
                    selected = false,
                    onClick = {
                        showResetDialog = true
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(appName) },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { padding ->

            val screenWidthPx = with(LocalContext.current.resources.displayMetrics) {
                widthPixels.toFloat()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val x = offset.x
                            when {
                                x < screenWidthPx * 0.2f -> {
                                    viewModel.prevPuzzle()
                                }
                                x > screenWidthPx * 0.8f -> {
                                    viewModel.nextPuzzle()
                                }
                                else -> {
                                    viewModel.toggleAnswer()
                                }
                            }
                        }
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // Основное прокручиваемое содержимое
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        puzzle?.let {
                            val descriptionResId = remember(it.descriptionKey) {
                                getStringResIdByName(context, it.descriptionKey)
                            }

                            if (descriptionResId != 0) {
                                Text(
                                    text = stringResource(id = descriptionResId),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }

                            PuzzleImageFromAssets(
                                fileName = if (showAnswer) it.a else it.q,
                                modifier = Modifier.fillMaxWidth()
                            )


                            Spacer(modifier = Modifier.height(8.dp))
                        } ?: Text(noPuzzlesLeft)
                    }

                    // Закреплённый блок снизу с ID и переключателем ответа
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        puzzle?.let {
                            Text(
                                text = "ID: ${it.id}",
                                style = MaterialTheme.typography.bodySmall
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (showAnswer) buttonShowPuzzle else buttonShowAnswer,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            if (showResetDialog) {
                AlertDialog(
                    onDismissRequest = { showResetDialog = false },
                    confirmButton = {
                        TextButton(onClick = {
                            showResetDialog = false
                            viewModel.resetProgress()
                        }) {
                            Text(dialogConfirm)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetDialog = false }) {
                            Text(dialogCancel)
                        }
                    },
                    title = { Text(dialogTitle) },
                    text = { Text(dialogMessage) }
                )
            }

            if (showAboutDialog) {
                AlertDialog(
                    onDismissRequest = { showAboutDialog = false },
                    confirmButton = {
                        TextButton(onClick = { showAboutDialog = false }) {
                            Text(dialogOk)
                        }
                    },
                    title = { Text(aboutText) },
                    text = {
                        Text("SavvyClub v1.0\nAuthor: rza res\n© 2025 All rights reserved.")
                    }
                )
            }
        }
    }
}

// Утилита получения ID строки по имени
fun getStringResIdByName(context: Context, name: String): Int {
    return context.resources.getIdentifier(name, "string", context.packageName)
}
