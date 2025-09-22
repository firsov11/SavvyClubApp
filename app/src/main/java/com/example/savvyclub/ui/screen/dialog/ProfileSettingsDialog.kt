package com.example.savvyclub.ui.screen.dialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.savvyclub.R
import com.example.savvyclub.viewmodel.AuthViewModel

@Composable
fun ProfileSettingsDialog(
    viewModel: AuthViewModel,
    onDismiss: () -> Unit,
    googleAvatarUrl: String? = null
) {
    val selectedAvatar by viewModel.selectedAvatar.collectAsState()

    val avatarList = listOf(
        R.drawable.bitcoin,
        R.drawable.dice,
        R.drawable.frog,
        R.drawable.linux,
        R.drawable.rubiks_cube
    )

    LaunchedEffect(googleAvatarUrl) {
        // если Google-аватар есть и локально он не установлен
        if (!googleAvatarUrl.isNullOrEmpty() && !selectedAvatar.startsWith("res:")) {
            viewModel.setSelectedAvatar(googleAvatarUrl)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 8.dp,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Выберите аватар", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))

                // Текущий аватар
                when {
                    selectedAvatar.startsWith("res:") -> {
                        val resId = selectedAvatar.removePrefix("res:").toIntOrNull()
                            ?: R.drawable.default_avatar
                        Image(
                            painter = painterResource(id = resId),
                            contentDescription = "Avatar",
                            modifier = Modifier.size(72.dp).clip(CircleShape)
                        )
                    }
                    selectedAvatar.isNotEmpty() -> {
                        AsyncImage(
                            model = selectedAvatar,
                            contentDescription = "Avatar",
                            modifier = Modifier.size(72.dp).clip(CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Локальные аватары
                avatarList.chunked(3).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        row.forEach { res ->
                            val isSelected = selectedAvatar == "res:$res"
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                        else Color.Transparent
                                    )
                                    .clickable { viewModel.setSelectedAvatar("res:$res") }
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

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = onDismiss) {
                    Text("Закрыть")
                }
            }
        }
    }
}





