package com.example.techfeed.feature.news.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.techfeed.R

@Composable
fun ArticleFilterRow(
    label: String,
    options: List<String>,
    selected: String,
    select: (String) -> Unit,
) {
    val largeFont = LocalDensity.current.fontScale > 1.3f
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (largeFont || maxWidth < 360.dp) {
            Column(Modifier.padding(start = 16.dp)) {
                Text(label, style = MaterialTheme.typography.labelMedium)
                FilterOptions(label, options, selected, select)
            }
        } else {
            Row(
                Modifier.padding(start = 16.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.width(64.dp),
                )
                FilterOptions(label, options, selected, select)
            }
        }
    }
}

@Composable
private fun FilterOptions(
    label: String,
    options: List<String>,
    selected: String,
    select: (String) -> Unit,
) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                modifier = Modifier.testTag("filter:$label:$option"),
                selected = selected == option,
                onClick = { select(option) },
                label = { Text(if (option == "すべて") stringResource(R.string.all) else option) },
            )
        }
    }
}
