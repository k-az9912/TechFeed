package com.example.techfeed.feature.translation.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun ModelDownloadDialog(confirm: () -> Unit, dismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("翻訳モデルをダウンロード") },
        text = {
            Text(
                "英語から日本語への端末内翻訳に必要なモデルを取得します。モバイル通信を使用する場合があります。Wi-Fi 環境での取得をおすすめします。取得後はこの確認を表示せず翻訳できます。",
            )
        },
        confirmButton = { TextButton(onClick = confirm) { Text("ダウンロード") } },
        dismissButton = { TextButton(onClick = dismiss) { Text("キャンセル") } },
    )
}
