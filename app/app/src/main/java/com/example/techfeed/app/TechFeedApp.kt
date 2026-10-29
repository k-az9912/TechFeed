package com.example.techfeed.app

import com.example.techfeed.feature.news.ui.NewsScreen
import com.example.techfeed.feature.news.ui.NewsViewModel
import com.example.techfeed.feature.settings.model.NewsSettings
import com.example.techfeed.feature.settings.ui.NewsSettingsScreen
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder

@Composable
fun TechFeedApp(model: NewsViewModel = hiltViewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    val saved by model.saved.collectAsStateWithLifecycle()
    val settings by model.settings.collectAsStateWithLifecycle()
    val translations by model.translations.collectAsStateWithLifecycle()
    val modelState by model.modelState.collectAsStateWithLifecycle()
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
                downloadModel = { article -> model.downloadModel(article) }
            )
        },
        settings = { back ->
            NewsSettingsScreen(
                settings, state.error, model::setSource, model::setTag,
                model::moveSource, model::moveTag, modelState,
                { model.downloadModel() }, back
            )
        }
    )
}

@Composable
internal fun TechFeedScreens(
    news: @Composable (openSettings: () -> Unit) -> Unit,
    settings: @Composable (back: () -> Unit) -> Unit
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
