package com.example.savvyclub.ui.screen

import android.widget.Toast
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
import com.example.savvyclub.data.model.PuzzleSource
import com.example.savvyclub.ui.component.PuzzleImageFromPath
import com.example.savvyclub.util.clearLocalPuzzlesFolder
import com.example.savvyclub.viewmodel.SavvyClubViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzleScreen(viewModel: SavvyClubViewModel) {
    val context = LocalContext.current

    val currentPuzzleItem by viewModel.currentPuzzle.collectAsState(initial = null)
    val currentIndex by viewModel.currentIndex.collectAsState(initial = 0)
    val showAnswer by viewModel.showAnswer.collectAsState(initial = false)
    val puzzles by viewModel.puzzles.collectAsState(initial = emptyList())
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val unpackProgress by viewModel.unpackProgress.collectAsState()
    val isUpdating by viewModel.isUpdating.collectAsState()

    val scrollState = rememberScrollState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showAboutDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.menu_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.about)) },
                    selected = false,
                    onClick = {
                        showAboutDialog = true
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Сбросить прогресс") },
                    selected = false,
                    onClick = {
                        showResetConfirmDialog = true
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Очистить память") },
                    selected = false,
                    onClick = {
                        clearLocalPuzzlesFolder(context)
                        Toast.makeText(context, "Память очищена", Toast.LENGTH_SHORT).show()
                        scope.launch { drawerState.close() }
                    }
                )

                if (showResetConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showResetConfirmDialog = false },
                        title = { Text("Сброс прогресса") },
                        text = { Text("Вы уверены, что хотите сбросить прогресс?") },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.resetProgress()
                                showResetConfirmDialog = false
                            }) { Text("Да") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showResetConfirmDialog = false }) { Text("Нет") }
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { padding ->
            val screenWidthPx = with(context.resources.displayMetrics) { widthPixels.toFloat() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val x = offset.x
                            when {
                                x < screenWidthPx * 0.2f -> viewModel.prevPuzzle()
                                x > screenWidthPx * 0.8f -> viewModel.nextPuzzle()
                                else -> viewModel.toggleAnswer()
                            }
                        }
                    }
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // --- Прогресс загрузки/распаковки ---
                    if (isUpdating) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "Загрузка: ${(downloadProgress * 100).toInt()}%")
                            LinearProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Распаковка: ${(unpackProgress * 100).toInt()}%")
                            LinearProgressIndicator(
                                progress = { unpackProgress },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // --- Основной контент паззла ---
                    currentPuzzleItem?.let { puzzleItem ->
                        val puzzle = puzzleItem.puzzle
                        val lang = Locale.getDefault().language
                        val textToShow = if (showAnswer) {
                            puzzle.answer[lang] ?: puzzle.answer["en"] ?: ""
                        } else {
                            puzzle.question[lang] ?: puzzle.question["en"] ?: ""
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Top
                        ) {
                            if (textToShow.isNotEmpty()) {
                                Text(
                                    text = textToShow,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }

                            val imagePath = if (showAnswer) puzzle.a else puzzle.q

                            PuzzleImageFromPath(
                                filePath = imagePath,
                                source = puzzleItem.source,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (showAnswer) "Ответ" else "Вопрос",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(8.dp)
                            )

                            Text(
                                text = "ID: ${puzzle.id}  (${currentIndex + 1} / ${puzzles.size})",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } ?: Text(
                        text = stringResource(R.string.no_puzzles_left),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            if (showAboutDialog) {
                AlertDialog(
                    onDismissRequest = { showAboutDialog = false },
                    confirmButton = {
                        TextButton(onClick = { showAboutDialog = false }) {
                            Text(stringResource(R.string.dialog_ok))
                        }
                    },
                    title = { Text(stringResource(R.string.about)) },
                    text = { Text("SavvyClub v1.0\nAuthor: rza res\n© 2025 All rights reserved.") }
                )
            }
        }
    }
}
