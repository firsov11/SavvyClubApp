package com.example.savvyclub.ui.screen.dialog

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.savvyclub.R
import com.example.savvyclub.viewmodel.SavvyClubViewModel

/**
 * Диалог подтверждения сброса прогресса
 * @param viewModel для вызова метода resetProgress()
 * @param onDismiss вызывается при закрытии диалога
 */
@Composable
fun ResetProgressDialog(
    viewModel: SavvyClubViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title)) },
        text = { Text(stringResource(R.string.dialog_message)) },
        confirmButton = {
            TextButton(onClick = {
                viewModel.resetProgress()
                onDismiss()
            }) {
                Text(stringResource(R.string.dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}
