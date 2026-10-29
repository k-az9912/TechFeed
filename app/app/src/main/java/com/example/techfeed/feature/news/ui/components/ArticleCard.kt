package com.example.techfeed.feature.news.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.techfeed.core.model.Article

@Composable
fun ArticleCard(
    article: Article,
    tags: List<String>,
    saved: Boolean,
    openArticle: () -> Unit,
    toggleSaved: () -> Unit,
) {
    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().clickable { openArticle() },
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "${article.source}  ·  ${article.time}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                article.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                article.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    tags.joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
                TextButton(onClick = { toggleSaved() }) { Text(if (saved) "★ 保存済み" else "☆ 保存") }
            }
        }
    }
}
