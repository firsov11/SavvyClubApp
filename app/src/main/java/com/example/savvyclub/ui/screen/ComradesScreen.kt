package com.example.savvyclub.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.savvyclub.R
import com.example.savvyclub.data.model.Comrade
import com.example.savvyclub.viewmodel.ComradesViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComradesScreen(
    viewModel: ComradesViewModel,
    onBack: () -> Unit
) {
    val comrades by viewModel.comrades.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Загружаем один раз при входе на экран
    LaunchedEffect(Unit) {
        viewModel.loadComrades()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Товарищи") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->

        SwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing),
            onRefresh = {
                isRefreshing = true
                viewModel.loadComrades {
                    isRefreshing = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(comrades) { comrade ->
                    ComradeItem(comrade)
                    Divider()
                }
            }
        }
    }
}

@Composable
fun ComradeItem(comrade: Comrade) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        // Аватар
        when {
            comrade.avatarUrl.startsWith("res:") -> {
                val resId = comrade.avatarUrl.removePrefix("res:").toIntOrNull() ?: R.drawable.default_avatar
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(CircleShape)
                )
            }
            comrade.avatarUrl.isNotEmpty() -> {
                AsyncImage(
                    model = comrade.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(CircleShape)
                )
            }
            else -> {
                Image(
                    painter = painterResource(id = R.drawable.default_avatar),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(comrade.name.ifEmpty { "(Без имени)" }, style = MaterialTheme.typography.bodyLarge)
            Text(comrade.email, style = MaterialTheme.typography.bodySmall)
        }

        // Индикатор онлайн/оффлайн
        val statusColor = if (comrade.isOnline)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)

        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(statusColor)
        )
    }
}
