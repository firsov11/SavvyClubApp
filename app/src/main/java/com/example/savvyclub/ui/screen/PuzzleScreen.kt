package com.example.savvyclub.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.savvyclub.R
import com.example.savvyclub.ui.component.PuzzleImageFromAssets
import com.example.savvyclub.viewmodel.SavvyClubViewModel
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzleScreen(viewModel: SavvyClubViewModel) {
    val puzzle by viewModel.currentPuzzle.collectAsState()
    val showAnswer by viewModel.showAnswer.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    TextButton(onClick = { showResetDialog = true }) {
                        Text(
                            text = stringResource(R.string.button_reset),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        bottomBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = viewModel::prevPuzzle) {
                    Text(stringResource(R.string.button_prev))
                }
                Button(onClick = viewModel::toggleAnswer) {
                    Text(
                        if (showAnswer)
                            stringResource(R.string.button_show_puzzle)
                        else
                            stringResource(R.string.button_show_answer)
                    )
                }
                Button(onClick = viewModel::nextPuzzle) {
                    Text(stringResource(R.string.button_next))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            puzzle?.let {
                PuzzleImageFromAssets(
                    fileName = if (showAnswer) it.a else it.q,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ID: ${it.id}",
                    style = MaterialTheme.typography.bodyMedium
                )
            } ?: Text(stringResource(R.string.no_puzzles_left))
        }



        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        showResetDialog = false
                        viewModel.resetProgress()
                    }) {
                        Text(stringResource(R.string.dialog_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                },
                title = {
                    Text(stringResource(R.string.dialog_title))
                },
                text = {
                    Text(stringResource(R.string.dialog_message))
                }
            )
        }
    }
}

