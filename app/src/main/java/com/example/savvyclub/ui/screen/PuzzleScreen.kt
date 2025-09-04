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
import com.example.savvyclub.ui.component.PuzzleImageFromPath
import com.example.savvyclub.util.clearLocalPuzzlesFolder
import com.example.savvyclub.viewmodel.SavvyClubViewModel
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically

import androidx.compose.material3.HorizontalDivider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzleScreen(viewModel: SavvyClubViewModel) {
    val context = LocalContext.current // Получаем контекст для Toast и файлов

    // -------------------- Подписка на состояния ViewModel --------------------
    // Compose будет автоматически обновлять UI при изменении этих состояний
    val currentPuzzleItem by viewModel.currentPuzzle.collectAsState(initial = null) // Текущий пазл
    val currentIndex by viewModel.currentIndex.collectAsState(initial = 0) // Индекс текущего пазла
    val showAnswer by viewModel.showAnswer.collectAsState(initial = false) // Показывать ли ответ
    val puzzles by viewModel.puzzles.collectAsState(initial = emptyList()) // Отфильтрованные пазлы
    val selectedTypes by viewModel.selectedTypes.collectAsState() // Выбранные фильтры
    val allPuzzles by viewModel.allPuzzles.collectAsState(initial = emptyList()) // Все пазлы без фильтрации

    val scrollState = rememberScrollState() // Состояние вертикального скролла
    val drawerState = rememberDrawerState(DrawerValue.Closed) // Состояние бокового меню
    val scope = rememberCoroutineScope() // CoroutineScope для управления Drawer

    // -------------------- Состояния диалогов и секций --------------------
    var showOpeningRemarksDialog by remember { mutableStateOf(false) } // Диалог "Вступительное слово"
    var showAboutDialog by remember { mutableStateOf(false) } // Диалог "О программе"
    var showResetConfirmDialog by remember { mutableStateOf(false) } // Подтверждение сброса прогресса
    var showFilterSection by remember { mutableStateOf(false) } // Секция фильтров в Drawer
    var showClearMemoryConfirmDialog by remember { mutableStateOf(false) } // Диалог очистки локальной памяти

    // Получаем уникальные типы пазлов для фильтров
    val allTypes = remember(allPuzzles) { allPuzzles.map { it.puzzle.type }.distinct() }

    // -------------------- Локализация типов --------------------
    val typeLocalizationMap = mapOf(
        "cryptorhyme" to R.string.type_cryptorhyme,
        "differences" to R.string.type_differences,
        "matches" to R.string.type_matches,
        "math" to R.string.type_math,
        "chess" to R.string.type_chess
    )

    // -------------------- Автосброс индекса --------------------
    // Если после фильтрации текущий индекс вышел за пределы списка
    LaunchedEffect(puzzles.size, currentIndex) {
        if (puzzles.isNotEmpty() && currentIndex >= puzzles.size) {
            viewModel.resetIndex() // Сбрасываем индекс на последний допустимый
        }
    }

    // -------------------- Боковое меню (Drawer) --------------------
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

                // -------------------- Вступительное слово --------------------
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.opening_remarks)) },
                    selected = false,
                    onClick = {
                        showOpeningRemarksDialog = true
                        scope.launch { drawerState.close() } // Закрываем Drawer
                    }
                )

                HorizontalDivider()

                // -------------------- Секция фильтров --------------------
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.filters)) },
                    selected = showFilterSection,
                    onClick = { showFilterSection = !showFilterSection } // Открытие/закрытие секции
                )

                // Анимированная видимость секции фильтров
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
                                    onCheckedChange = { viewModel.toggleFilter(type) } // Изменяем фильтр
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

                HorizontalDivider()

                // -------------------- Другие пункты меню --------------------
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.about)) },
                    selected = false,
                    onClick = {
                        showAboutDialog = true
                        scope.launch { drawerState.close() } // Закрываем Drawer
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

                // -------------------- Диалог очистки памяти --------------------
                if (showClearMemoryConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showClearMemoryConfirmDialog = false },
                        title = { Text(stringResource(R.string.clear_memory)) },
                        text = { Text(stringResource(R.string.confirm_clear_memory)) },
                        confirmButton = {
                            TextButton(onClick = {
                                clearLocalPuzzlesFolder(context) // Очистка локальной папки
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.memory_cleared),
                                    Toast.LENGTH_SHORT
                                ).show()
                                showClearMemoryConfirmDialog = false
                            }) { Text(stringResource(R.string.confirm)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showClearMemoryConfirmDialog = false }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    )
                }

                // -------------------- Диалог сброса прогресса --------------------
                if (showResetConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showResetConfirmDialog = false },
                        title = { Text(stringResource(R.string.dialog_title)) },
                        text = { Text(stringResource(R.string.dialog_message)) },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.resetProgress()
                                showResetConfirmDialog = false
                            }) { Text(stringResource(R.string.dialog_confirm)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showResetConfirmDialog = false }) {
                                Text(stringResource(R.string.dialog_cancel))
                            }
                        }
                    )
                }
            }
        }
    ) {

        // -------------------- Основной Scaffold --------------------
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

            // -------------------- Основная зона кликов --------------------
            // Свайпы и тап по экрану:
            // - Левая часть -> предыдущий пазл
            // - Правая часть -> следующий пазл
            // - Средняя часть -> показать/скрыть ответ
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
                    // -------------------- Основной контент пазла --------------------
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
                            // Текст вопроса или ответа
                            if (textToShow.isNotEmpty()) {
                                Text(
                                    text = textToShow,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }

                            // Изображение пазла
                            val imagePath = if (showAnswer) puzzle.a else puzzle.q
                            PuzzleImageFromPath(
                                filePath = imagePath,
                                source = puzzleItem.source,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Кнопка показать/скрыть ответ
                            Text(
                                text = if (showAnswer) stringResource(R.string.button_show_answer)
                                else stringResource(R.string.button_show_puzzle),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(8.dp)
                            )

                            // Информация о текущем пазле
                            Text(
                                text = "ID: ${puzzle.id}  (${currentIndex + 1} / ${puzzles.size})",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } ?: run {
                        // Если пазлов больше нет
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

            // -------------------- Диалог "Вступительное слово" --------------------
            if (showOpeningRemarksDialog) {
                AlertDialog(
                    onDismissRequest = { showOpeningRemarksDialog = false },
                    confirmButton = {
                        TextButton(onClick = { showOpeningRemarksDialog = false }) {
                            Text(stringResource(R.string.dialog_ok))
                        }
                    },
                    title = { Text(stringResource(R.string.opening_remarks)) },
                    text = { Text(stringResource(R.string.opening_remarks_txt)) }
                )
            }

            // -------------------- Диалог "О программе" --------------------
            if (showAboutDialog) {
                AlertDialog(
                    onDismissRequest = { showAboutDialog = false },
                    confirmButton = {
                        TextButton(onClick = { showAboutDialog = false }) {
                            Text(stringResource(R.string.dialog_ok))
                        }
                    },
                    title = { Text(stringResource(R.string.about)) },
                    text = { Text("SavvyClub v1.0\nAuthor: rza\n2025") }
                )
            }
        }
    }
}
