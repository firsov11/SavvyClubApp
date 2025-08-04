package com.example.savvyclub.ui.screen

import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
    val currentPuzzleItem by viewModel.currentPuzzle.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val showAnswer by viewModel.showAnswer.collectAsState()
    val scrollState = rememberScrollState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val puzzles by viewModel.puzzles.collectAsState()

    var showAboutDialog by remember { mutableStateOf(false) }

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
                        viewModel.resetProgress()
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


