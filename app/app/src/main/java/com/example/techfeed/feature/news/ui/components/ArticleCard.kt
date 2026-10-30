package com.example.techfeed.feature.news.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.techfeed.R
import com.example.techfeed.core.model.Article
import com.example.techfeed.core.time.formatArticleTime

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
                stringResource(
                    R.string.article_metadata,
                    article.source,
                    formatArticleTime(article.time) ?: stringResource(R.string.date_unknown),
                ),
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
                    modifier = Modifier.weight(1f).padding(top = 12.dp, end = 8.dp),
                )
                TextButton(onClick = { toggleSaved() }) {
                    Text(
                        if (saved) stringResource(R.string.article_saved)
                        else stringResource(R.string.save_article),
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}
