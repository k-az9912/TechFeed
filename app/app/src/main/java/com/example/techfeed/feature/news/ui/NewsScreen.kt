package com.example.techfeed.feature.news.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.techfeed.R
import com.example.techfeed.core.model.Article
import com.example.techfeed.core.tags.articleTags
import com.example.techfeed.feature.news.domain.filterArticles
import com.example.techfeed.feature.news.ui.components.ArticleCard
import com.example.techfeed.feature.news.ui.components.ArticleFilterRow
import com.example.techfeed.feature.settings.model.NewsSettings
import com.example.techfeed.feature.translation.model.ArticleTranslation
import com.example.techfeed.feature.translation.model.TranslationModelState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
    state: NewsState = NewsState(),
    saved: List<Article> = emptyList(),
    refresh: () -> Unit = {},
    toggleSaved: (Article) -> Unit = {},
    settings: NewsSettings = NewsSettings(),
    translations: Map<String, ArticleTranslation> = emptyMap(),
    translate: (Article) -> Unit = {},
    openSettings: () -> Unit = {},
    modelState: TranslationModelState = TranslationModelState(),
    downloadModel: (Article) -> Unit = {},
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var category by rememberSaveable { mutableStateOf("すべて") }
    var selectedSource by rememberSaveable { mutableStateOf("すべて") }
    val savedIds = saved.map { it.id }
    val listState = rememberLazyListState()
    var filtersExpanded by rememberSaveable { mutableStateOf(true) }
    val scrollThreshold = with(LocalDensity.current) { 24.dp.toPx() }
    val filterScrollConnection =
        remember(listState, scrollThreshold) {
            object : NestedScrollConnection {
                private var distance = 0f

                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (source == NestedScrollSource.UserInput && available.y != 0f) {
                        if (distance * available.y < 0f) distance = 0f
                        distance += available.y
                        if (distance < -scrollThreshold && listState.canScrollForward) {
                            filtersExpanded = false
                            distance = 0f
                        } else if (distance > scrollThreshold) {
                            filtersExpanded = true
                            distance = 0f
                        }
                    }
                    return Offset.Zero
                }
            }
        }
    var selectedArticleId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedArticle = (saved + state.articles).firstOrNull { it.id == selectedArticleId }
    val categories = listOf("すべて") + settings.tagOrder.filter { it in settings.visibleTags }
    val sources =
        listOf("すべて") +
            settings.sourceOrder.filter {
                selectedTab == 1 || it in settings.sources
            }
    val activeSource = selectedSource.takeIf { it in sources } ?: "すべて"
    val activeTag = category.takeIf { it in categories } ?: "すべて"
    val visible =
        filterArticles(if (selectedTab == 0) state.articles else saved, activeSource, activeTag)

    val filterSelection = listOf(selectedTab.toString(), activeSource, activeTag)
    var previousSelection by rememberSaveable { mutableStateOf(filterSelection) }
    LaunchedEffect(filterSelection) {
        if (previousSelection != filterSelection) {
            filtersExpanded = true
            listState.scrollToItem(0)
            previousSelection = filterSelection
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selectedTab == 0) stringResource(R.string.app_name)
                        else stringResource(R.string.saved_title),
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            selectedTab = if (selectedTab == 0) 1 else 0
                            category = "すべて"
                            selectedSource = "すべて"
                        },
                    ) {
                        Icon(
                            painterResource(
                                if (selectedTab == 0) R.drawable.ic_bookmark
                                else R.drawable.ic_news,
                            ),
                            contentDescription =
                                if (selectedTab == 0) stringResource(R.string.saved_articles)
                                else stringResource(R.string.news_list),
                        )
                    }
                    IconButton(onClick = refresh, enabled = !state.loading) {
                        Icon(
                            painterResource(R.drawable.ic_refresh),
                            contentDescription = stringResource(R.string.refresh_news),
                        )
                    }
                    IconButton(onClick = openSettings) {
                        Icon(
                            painterResource(R.drawable.ic_settings),
                            contentDescription = stringResource(R.string.settings),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error?.let {
                Text(
                    it.resolve(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
            }
            AnimatedVisibility(
                visible = filtersExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(Modifier.testTag("news_filters")) {
                    ArticleFilterRow(
                        stringResource(R.string.filter_source),
                        sources,
                        activeSource,
                    ) {
                        selectedSource = it
                    }
                    ArticleFilterRow(stringResource(R.string.filter_tag), categories, activeTag) {
                        category = it
                    }
                }
            }
            if (visible.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().padding(32.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        if (state.loading) stringResource(R.string.loading)
                        else stringResource(R.string.no_articles),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        if (selectedTab == 1) stringResource(R.string.empty_saved_hint)
                        else if (settings.sources.isEmpty())
                            stringResource(R.string.empty_sources_hint)
                        else stringResource(R.string.empty_news_hint),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier =
                        Modifier.weight(1f)
                            .testTag("news_list")
                            .nestedScroll(filterScrollConnection),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(visible, key = { it.id }) { article ->
                        ArticleCard(
                            article = article,
                            tags =
                                settings.tagOrder.filter {
                                    it in settings.visibleTags && it in articleTags(article)
                                },
                            saved = article.id in savedIds,
                            openArticle = { selectedArticleId = article.id },
                            toggleSaved = { toggleSaved(article) },
                        )
                    }
                }
            }
        }
    }
    if (selectedArticle != null) {
        ArticleDetailDialog(
            article = selectedArticle,
            translation = translations[selectedArticle.id] ?: ArticleTranslation(),
            modelState = modelState,
            downloadModel = { downloadModel(selectedArticle) },
            translate = { translate(selectedArticle) },
            dismiss = { selectedArticleId = null },
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun TechFeedPreview() {
    MaterialTheme {
        NewsScreen(
            NewsState(
                articles =
                    listOf(
                        Article(
                            "preview",
                            "Android Developers",
                            "Android",
                            "Compose の画面サンプル",
                            "プレビュー用のサンプルです。実際のニュースではありません。",
                            "2026-10-28",
                            "https://developer.android.com/",
                        ),
                    ),
            ),
        )
    }
}
