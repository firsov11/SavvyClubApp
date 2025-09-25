package com.example.savvyclub.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.savvyclub.R
import com.example.savvyclub.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val userState by authViewModel.userState.collectAsState()
    val selectedAvatar = userState?.avatarUrl ?: "res:${R.drawable.default_avatar}"
    val userName = userState?.name ?: ""
    var name by remember { mutableStateOf(TextFieldValue(userName)) }

    LaunchedEffect(userName) { name = TextFieldValue(userName) }

    val avatarList = listOf(
        R.drawable.bitcoin,
        R.drawable.forty_four,
        R.drawable.black_king,
        R.drawable.dice,
        R.drawable.frog,
        R.drawable.black_queen,
        R.drawable.linux,
        R.drawable.rubiks_cube,
        R.drawable.graffiti
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки профиля") },
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Аватар
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    if (selectedAvatar.startsWith("res:")) {
                        val resId = selectedAvatar.removePrefix("res:").toIntOrNull() ?: R.drawable.default_avatar
                        Image(
                            painter = painterResource(id = resId),
                            contentDescription = "Avatar",
                            modifier = Modifier.size(96.dp).clip(CircleShape)
                        )
                    } else if (selectedAvatar.isNotEmpty()) {
                        AsyncImage(
                            model = selectedAvatar,
                            contentDescription = "Avatar",
                            modifier = Modifier.size(96.dp).clip(CircleShape)
                        )
                    }
                }
            }

            // Выбор аватаров
            avatarList.chunked(4).forEach { row ->
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        row.forEach { res ->
                            val isSelected = selectedAvatar == "res:$res"
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        authViewModel.saveLocally(
                                            userState!!.copy(avatarUrl = "res:$res")
                                        )
                                    }
                            ) {
                                Image(
                                    painter = painterResource(id = res),
                                    contentDescription = "Avatar",
                                    modifier = Modifier.size(64.dp).clip(CircleShape)
                                )
                            }
                        }
                    }
                }
            }

            // Имя пользователя
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Кнопка Сохранить
            item {
                Button(
                    onClick = {
                        userState?.let {
                            authViewModel.saveLocally(
                                it.copy(
                                    name = name.text,
                                    avatarUrl = selectedAvatar
                                )
                            )
                        }
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Сохранить")
                }
            }
        }
    }
}
