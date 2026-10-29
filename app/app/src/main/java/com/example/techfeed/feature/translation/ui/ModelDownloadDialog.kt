package com.example.techfeed.feature.translation.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.techfeed.R

@Composable
fun ModelDownloadDialog(confirm: () -> Unit, dismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(stringResource(R.string.download_model)) },
        text = { Text(stringResource(R.string.download_model_message)) },
        confirmButton = {
            TextButton(onClick = confirm) { Text(stringResource(R.string.download)) }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
