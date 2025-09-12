package com.example.savvyclub.ui.screen

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.savvyclub.R
import com.example.savvyclub.ui.component.PuzzleImageFromPath
import com.example.savvyclub.ui.dialog.*
import com.example.savvyclub.viewmodel.AuthViewModel
import com.example.savvyclub.viewmodel.SavvyClubViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzleScreen(
    viewModel: SavvyClubViewModel,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val activity = context as Activity
    val scope = rememberCoroutineScope()

    // -------------------- Авторизация --------------------
    var showLoginDialog by remember { mutableStateOf(false) }
    val userEmail by authViewModel.userState.collectAsState()

    // -------------------- Подписка на состояния ViewModel --------------------
    val currentPuzzleItem by viewModel.currentPuzzle.collectAsState(initial = null)
    val currentIndex by viewModel.currentIndex.collectAsState(initial = 0)
    val showAnswer by viewModel.showAnswer.collectAsState(initial = false)
    val puzzles by viewModel.puzzles.collectAsState(initial = emptyList())
    val selectedTypes by viewModel.selectedTypes.collectAsState()
    val allPuzzles by viewModel.allPuzzles.collectAsState(initial = emptyList())
    val scrollState = rememberScrollState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    // -------------------- Диалоговые состояния --------------------
    var showOpeningRemarksDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showClearMemoryConfirmDialog by remember { mutableStateOf(false) }
    var showFilterSection by remember { mutableStateOf(false) }

    // -------------------- Уникальные типы для фильтров --------------------
    val allTypes = remember(allPuzzles) { allPuzzles.map { it.puzzle.type }.distinct() }

    // -------------------- Локализация типов --------------------
    val typeLocalizationMap = mapOf(
        "cryptorhyme" to R.string.type_cryptorhyme,
        "differences" to R.string.type_differences,
        "matches" to R.string.type_matches,
        "math" to R.string.type_math,
        "chess" to R.string.type_chess
    )

    // -------------------- Автосброс индекса при фильтрации --------------------
    LaunchedEffect(puzzles.size, currentIndex) {
        if (puzzles.isNotEmpty() && currentIndex >= puzzles.size) {
            viewModel.resetIndex()
        }
    }

    // -------------------- Drawer и боковое меню --------------------
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))

                // Приветствие и кнопки вход/выход
                if (userEmail != null) {
                    Text("Hello, $userEmail", modifier = Modifier.padding(16.dp))
                    Button(
                        onClick = { authViewModel.signOut() },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) { Text("Sign out") }
                } else {
                    Text("Hello, Guest", modifier = Modifier.padding(16.dp))
                    Button(
                        onClick = { showLoginDialog = true },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) { Text("Sign in / Register") }
                }

                Text(
                    text = stringResource(R.string.menu_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )

                Divider()

                // 🔹 Вступительное слово
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.opening_remarks)) },
                    selected = false,
                    onClick = {
                        showOpeningRemarksDialog = true
                        scope.launch { drawerState.close() }
                    }
                )

                Divider()

                // 🔹 Фильтры
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.filters)) },
                    selected = showFilterSection,
                    onClick = { showFilterSection = !showFilterSection }
                )

                AnimatedVisibility(
                    visible = showFilterSection,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp)) {
                        allTypes.forEach { type ->
                            val checked = type in selectedTypes
                            val localizedType = typeLocalizationMap[type]?.let { stringResource(it) }
                                ?: type.replaceFirstChar { it.uppercase() }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { viewModel.toggleFilter(type) }
                                )
                                Text(
                                    text = localizedType,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }

                Divider()

                // 🔹 Остальные пункты меню
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.about)) },
                    selected = false,
                    onClick = {
                        showAboutDialog = true
                        scope.launch { drawerState.close() }
                    }
                )

                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.reset_progress)) },
                    selected = false,
                    onClick = {
                        showResetConfirmDialog = true
                        scope.launch { drawerState.close() }
                    }
                )

                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.clear_memory)) },
                    selected = false,
                    onClick = {
                        showClearMemoryConfirmDialog = true
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
                // 🔹 Фон
                Image(
                    painter = painterResource(R.drawable.bg_puzzles),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.11f,
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
                )

                // -------------------- Основной контент --------------------
                Column(modifier = Modifier.fillMaxSize()) {
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
                                text = if (showAnswer) stringResource(R.string.button_show_answer)
                                else stringResource(R.string.button_show_puzzle),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(8.dp)
                            )

                            Text(
                                text = "ID: ${puzzle.id}  (${currentIndex + 1} / ${puzzles.size})",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } ?: run {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_puzzles_left),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }

            // -------------------- Диалоги --------------------
            if (showAboutDialog) AboutDialog { showAboutDialog = false }
            if (showOpeningRemarksDialog) OpeningRemarksDialog { showOpeningRemarksDialog = false }
            if (showClearMemoryConfirmDialog) ClearMemoryDialog(context) { showClearMemoryConfirmDialog = false }
            if (showResetConfirmDialog) ResetProgressDialog(viewModel) { showResetConfirmDialog = false }
            if (showLoginDialog) {
                LoginDialog(authViewModel) { showLoginDialog = false }
            }
        }
    }
}
