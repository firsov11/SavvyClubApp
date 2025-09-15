package com.example.savvyclub.ui.screen.dialog

import android.content.Context
import android.widget.Toast
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.savvyclub.R
import com.example.savvyclub.util.clearLocalPuzzlesFolder

/**
 * Диалог подтверждения очистки локальной памяти
 * @param context для вызова Toast
 * @param onDismiss вызывается при закрытии диалога
 */
@Composable
fun ClearMemoryDialog(
    context: Context,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.clear_memory)) },
        text = { Text(stringResource(R.string.confirm_clear_memory)) },
        confirmButton = {
            TextButton(onClick = {
                clearLocalPuzzlesFolder(context) // Очистка папки
                Toast.makeText(
                    context,
                    context.getString(R.string.memory_cleared),
                    Toast.LENGTH_SHORT
                ).show()
                onDismiss()
            }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
