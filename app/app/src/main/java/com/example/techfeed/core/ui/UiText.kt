package com.example.techfeed.core.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

data class UiText(@param:StringRes val resourceId: Int, val arguments: List<String> = emptyList()) {
    @Composable fun resolve(): String = stringResource(resourceId, *arguments.toTypedArray())
}
