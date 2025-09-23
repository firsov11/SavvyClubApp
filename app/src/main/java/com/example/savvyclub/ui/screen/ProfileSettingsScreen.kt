package com.example.savvyclub.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.savvyclub.R
import com.example.savvyclub.viewmodel.AuthViewModel

@Composable
fun ProfileSettingsScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) { authViewModel.loadUserData() }

    val selectedAvatar by authViewModel.selectedAvatar.collectAsState()
    val userName by authViewModel.userName.collectAsState()
    var name by remember { mutableStateOf(TextFieldValue(userName ?: "")) }

    LaunchedEffect(userName) { name = TextFieldValue(userName ?: "") }

    val avatarList = listOf(R.drawable.bitcoin, R.drawable.dice, R.drawable.frog, R.drawable.linux, R.drawable.rubiks_cube)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Настройки профиля", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f))
            Text("Назад", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { onBack() })
        }

        Spacer(Modifier.height(24.dp))

        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            if (selectedAvatar.startsWith("res:")) {
                val resId = selectedAvatar.removePrefix("res:").toIntOrNull() ?: R.drawable.default_avatar
                Image(painter = painterResource(id = resId), contentDescription = "Avatar", modifier = Modifier.size(96.dp).clip(CircleShape))
            } else if (selectedAvatar.isNotEmpty()) {
                AsyncImage(model = selectedAvatar, contentDescription = "Avatar", modifier = Modifier.size(96.dp).clip(CircleShape))
            }
        }

        Spacer(Modifier.height(16.dp))

        avatarList.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                row.forEach { res ->
                    val isSelected = selectedAvatar == "res:$res"
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent)
                            .clickable { authViewModel.setSelectedAvatar("res:$res") }
                    ) {
                        Image(painter = painterResource(id = res), contentDescription = "Avatar", modifier = Modifier.size(64.dp).clip(CircleShape))
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Имя") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                authViewModel.setSelectedAvatar(selectedAvatar)
                authViewModel.saveNameForEmailUser(name.text)
                onBack()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Сохранить")
        }
    }
}
