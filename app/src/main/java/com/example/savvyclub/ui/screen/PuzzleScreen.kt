package com.example.savvyclub.ui.screen

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.savvyclub.R
import com.example.savvyclub.ui.component.PuzzleImageFromPath
import com.example.savvyclub.ui.screen.dialog.AboutDialog
import com.example.savvyclub.ui.screen.dialog.ClearMemoryDialog
import com.example.savvyclub.ui.screen.dialog.LoginDialog
import com.example.savvyclub.ui.screen.dialog.ResetProgressDialog
import com.example.savvyclub.viewmodel.AuthViewModel
import com.example.savvyclub.viewmodel.SavvyClubViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun PuzzleScreen(
    viewModel: SavvyClubViewModel,
    authViewModel: AuthViewModel,
    overlayScreen: MutableState<String?>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activity = context as? Activity


    var showLoginDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showClearMemoryConfirmDialog by remember { mutableStateOf(false) }
    var showFilterSection by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val userState by authViewModel.userState.collectAsState()
    val userEmail = userState?.email
    val userName = userState?.name ?: ""
    val avatar = userState?.avatarUrl ?: "res:${R.drawable.default_avatar}"

    val currentPuzzleItem by viewModel.currentPuzzle.collectAsState(initial = null)
    val currentIndex by viewModel.currentIndex.collectAsState(initial = 0)
    val showAnswer by viewModel.showAnswer.collectAsState(initial = false)
    val puzzles by viewModel.puzzles.collectAsState(initial = emptyList())
    val selectedTypes by viewModel.selectedTypes.collectAsState()
    val allPuzzles by viewModel.allPuzzles.collectAsState(initial = emptyList())
    val scrollState = rememberScrollState()

    val allTypes = remember(allPuzzles) { allPuzzles.map { it.puzzle.type }.distinct() }
    val typeLocalizationMap = mapOf(
        "cryptorhyme" to R.string.type_cryptorhyme,
        "differences" to R.string.type_differences,
        "matches" to R.string.type_matches,
        "math" to R.string.type_math,
        "chess" to R.string.type_chess
    )

    LaunchedEffect(puzzles.size, currentIndex) {
        if (puzzles.isNotEmpty() && currentIndex >= puzzles.size) viewModel.resetIndex()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 16.dp)
                ) {
                    Spacer(Modifier.height(12.dp))

                    // 🔹 Профиль
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                        if (avatar.startsWith("res:")) {
                            val resId = avatar.removePrefix("res:").toIntOrNull() ?: R.drawable.default_avatar
                            Image(
                                painter = painterResource(id = resId),
                                contentDescription = "Avatar",
                                modifier = Modifier.size(48.dp).clip(CircleShape)
                            )
                        } else {
                            AsyncImage(
                                model = avatar,
                                contentDescription = "Avatar",
                                modifier = Modifier.size(48.dp).clip(CircleShape),
                                placeholder = painterResource(R.drawable.default_avatar)
                            )
                        }

                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Hello, ${if (userName.isBlank()) "Guest" else userName}")
                            Button(
                                onClick = {
                                    if (userEmail != null && activity != null) {
                                        authViewModel.signOut(activity)
                                    } else {
                                        showLoginDialog = true
                                    }
                                },
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(if (userEmail != null) "Sign out" else "Sign in / Register")
                            }

                        }
                    }

                    // 🔹 Только для вошедших пользователей
                    if (userEmail != null) {
                        NavigationDrawerItem(
                            label = { Text("Настройки профиля") },
                            selected = false,
                            onClick = {
                                overlayScreen.value = "profile_settings"
                                scope.launch { drawerState.close() }
                            }
                        )

                        NavigationDrawerItem(
                            label = { Text("Товарищи") },
                            selected = false,
                            onClick = {
                                overlayScreen.value = "comrades"
                                scope.launch { drawerState.close() }
                            }
                        )
                    }

                    // 🔹 Доступно всем
                    NavigationDrawerItem(
                        label = { Text("Вступительное слово") },
                        selected = false,
                        onClick = {
                            overlayScreen.value = "opening_remarks"
                            scope.launch { drawerState.close() }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // 🔹 Фильтры
                    NavigationDrawerItem(label = { Text(stringResource(R.string.filters)) },
                        selected = showFilterSection,
                        onClick = { showFilterSection = !showFilterSection })

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
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Checkbox(checked = checked, onCheckedChange = { viewModel.toggleFilter(type) })
                                    Text(localizedType, modifier = Modifier.padding(start = 8.dp))
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    NavigationDrawerItem(label = { Text(stringResource(R.string.about)) }, selected = false,
                        onClick = {
                            showAboutDialog = true
                            scope.launch { drawerState.close() }
                        })

                    NavigationDrawerItem(label = { Text(stringResource(R.string.reset_progress)) }, selected = false,
                        onClick = {
                            showResetConfirmDialog = true
                            scope.launch { drawerState.close() }
                        })

                    NavigationDrawerItem(label = { Text(stringResource(R.string.clear_memory)) }, selected = false,
                        onClick = {
                            showClearMemoryConfirmDialog = true
                            scope.launch { drawerState.close() }
                        })
                }
            }
        }
    ) {
        // ---------------- Контент PuzzleScreen ----------------
        Scaffold(containerColor = MaterialTheme.colorScheme.background,
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
                    .background(MaterialTheme.colorScheme.background)
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
                Image(
                    painter = painterResource(R.drawable.bg_puzzles),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.11f,
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
                )

                Column(modifier = Modifier.fillMaxSize()) {
                    currentPuzzleItem?.let { puzzleItem ->
                        val puzzle = puzzleItem.puzzle
                        val lang = Locale.getDefault().language
                        val textToShow =
                            if (showAnswer) puzzle.answer[lang] ?: puzzle.answer["en"] ?: ""
                            else puzzle.question[lang] ?: puzzle.question["en"] ?: ""

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (textToShow.isNotEmpty()) Text(
                                textToShow,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )

                            PuzzleImageFromPath(
                                filePath = if (showAnswer) puzzle.a else puzzle.q,
                                source = puzzleItem.source,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(8.dp))

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
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                stringResource(R.string.no_puzzles_left),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

                // диалоги
                if (showAboutDialog) AboutDialog { showAboutDialog = false }
                if (showClearMemoryConfirmDialog) ClearMemoryDialog(context) { showClearMemoryConfirmDialog = false }
                if (showResetConfirmDialog) ResetProgressDialog(viewModel) { showResetConfirmDialog = false }
                if (showLoginDialog) LoginDialog(authViewModel, { showLoginDialog = false }, { showLoginDialog = false })
            }
        }
    }
}
