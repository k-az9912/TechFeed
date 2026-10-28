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

@Composable
fun TechFeedApp(model: NewsViewModel = hiltViewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    val saved by model.saved.collectAsStateWithLifecycle()
    val settings by model.settings.collectAsStateWithLifecycle()
    val translations by model.translations.collectAsStateWithLifecycle()
    val modelState by model.modelState.collectAsStateWithLifecycle()
    var showSettings by rememberSaveable { mutableStateOf(false) }
    BackHandler(showSettings) { showSettings = false }
    if (showSettings) {
        NewsSettingsScreen(settings, state.error, model::setSource, model::setTag,
            model::moveSource, model::moveTag, modelState, { model.downloadModel() }) { showSettings = false }
    } else {
        NewsScreen(state, saved, model::refresh, model::toggleSaved,
            settings ?: NewsSettings(), translations, model::translate, { showSettings = true },
            modelState, { article -> model.downloadModel(article) })
    }
}
