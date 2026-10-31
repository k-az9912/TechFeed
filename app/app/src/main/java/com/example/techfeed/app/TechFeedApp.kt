package com.example.techfeed.app

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.techfeed.feature.news.ui.NewsScreen
import com.example.techfeed.feature.news.ui.NewsViewModel
import com.example.techfeed.feature.settings.model.NewsSettings
import com.example.techfeed.feature.settings.ui.NewsSettingsScreen

@Composable
fun TechFeedApp(model: NewsViewModel = hiltViewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    val saved by model.saved.collectAsStateWithLifecycle()
    val settings by model.settings.collectAsStateWithLifecycle()
    val translations by model.translations.collectAsStateWithLifecycle()
    val modelState by model.modelState.collectAsStateWithLifecycle()
    val customFeedState by model.customFeedState.collectAsStateWithLifecycle()
    val customTagState by model.customTagState.collectAsStateWithLifecycle()
    TechFeedScreens(
        news = { openSettings ->
            NewsScreen(
                state = state,
                saved = saved,
                refresh = model::refresh,
                toggleSaved = model::toggleSaved,
                settings = settings ?: NewsSettings(),
                translations = translations,
                translate = model::translate,
                openSettings = openSettings,
                modelState = modelState,
                downloadModel = { article -> model.downloadModel(article) },
            )
        },
        settings = { back ->
            NewsSettingsScreen(
                settings = settings,
                error = state.error,
                setSource = model::setSource,
                setTag = model::setTag,
                moveSource = model::moveSource,
                moveTag = model::moveTag,
                customFeedState = customFeedState,
                addCustomFeed = model::addCustomFeed,
                removeCustomFeed = model::removeCustomFeed,
                customTagState = customTagState,
                addCustomTag = model::addCustomTag,
                removeCustomTag = model::removeCustomTag,
                modelState = modelState,
                downloadModel = { model.downloadModel() },
                back = back,
            )
        },
    )
}

@Composable
internal fun TechFeedScreens(
    news: @Composable (openSettings: () -> Unit) -> Unit,
    settings: @Composable (back: () -> Unit) -> Unit,
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val stateHolder = rememberSaveableStateHolder()
    BackHandler(showSettings) { showSettings = false }
    val screen = if (showSettings) "settings" else "news"
    stateHolder.SaveableStateProvider(screen) {
        if (showSettings) {
            settings { showSettings = false }
        } else {
            news { showSettings = true }
        }
    }
}
