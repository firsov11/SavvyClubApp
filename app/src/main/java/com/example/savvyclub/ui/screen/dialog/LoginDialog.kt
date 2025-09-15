package com.example.savvyclub.ui.screen.dialog

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.savvyclub.BuildConfig.GOOGLE_CLIENT_ID
import com.example.savvyclub.viewmodel.AuthViewModel

@Composable
fun LoginDialog(
    authViewModel: AuthViewModel,
    onDismiss: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val activity = context as Activity

    // Инициализация One Tap клиента
    LaunchedEffect(Unit) {
        authViewModel.initGoogleSignInClient(activity)
    }

    // Launcher для Google One Tap
    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        authViewModel.handleGoogleSignInResult(result.data) { success, message ->
            if (!success) errorMsg = message
            else onDismiss()
        }
    }

    // Функция для проверки формата email
    fun isValidEmail(input: String) =
        android.util.Patterns.EMAIL_ADDRESS.matcher(input).matches()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sign In / Register") },
        text = {
            Column {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth()
                )

                errorMsg?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        when {
                            email.isBlank() || password.isBlank() -> errorMsg = "Email и пароль не могут быть пустыми"
                            !isValidEmail(email) -> errorMsg = "Введите корректный email"
                            password.length < 6 -> errorMsg = "Пароль должен быть не менее 6 символов"
                            else -> authViewModel.signInWithEmail(email, password) { success, msg ->
                                if (success) onDismiss() else errorMsg = msg
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Sign In") }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        when {
                            email.isBlank() || password.isBlank() -> errorMsg = "Email и пароль не могут быть пустыми"
                            !isValidEmail(email) -> errorMsg = "Введите корректный email"
                            password.length < 6 -> errorMsg = "Пароль должен быть не менее 6 символов"
                            else -> authViewModel.signUpWithEmail(email, password) { success, msg ->
                                if (success) onDismiss() else errorMsg = msg
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Register") }


                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        val request = authViewModel.getSignInRequest(clientId = GOOGLE_CLIENT_ID)
                        authViewModel.startGoogleSignIn(
                            request,
                            onSuccess = { intentSenderRequest -> googleLauncher.launch(intentSenderRequest) },
                            onFailure = { errorMsg = it }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Sign In with Google") }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

