package com.example.savvyclub.ui.screen.dialog

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.savvyclub.R

/**
 * Диалог "Вступительное слово"
 * @param onDismiss вызывается при закрытии диалога
 */
@Composable
fun OpeningRemarksDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_ok)) // Кнопка "OK"
            }
        },
        title = { Text(stringResource(R.string.opening_remarks)) },
        text = { Text(stringResource(R.string.opening_remarks_txt)) }
    )
}
