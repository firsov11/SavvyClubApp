package com.example.savvyclub.ui.screen.dialog

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.savvyclub.R

/**
 * Диалог "О программе"
 * @param onDismiss вызывается при закрытии диалога
 */
@Composable
fun AboutDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_ok)) // Кнопка "OK"
            }
        },
        title = { Text(stringResource(R.string.about)) },
        text = {
            Text("SavvyClub v1.0\nAuthor: rza\n2025")
        }
    )
}
