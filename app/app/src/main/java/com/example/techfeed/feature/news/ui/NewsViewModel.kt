package com.example.techfeed.feature.news.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techfeed.R
import com.example.techfeed.core.feed.feedSources
import com.example.techfeed.core.feed.isEnglishArticle
import com.example.techfeed.core.model.Article
import com.example.techfeed.core.ui.UiText
import com.example.techfeed.feature.news.data.NewsRepository
import com.example.techfeed.feature.news.domain.resolveFeedResults
import com.example.techfeed.feature.settings.data.SettingsRepository
import com.example.techfeed.feature.settings.model.NewsSettings
import com.example.techfeed.feature.translation.domain.TranslationService
import com.example.techfeed.feature.translation.model.ArticleTranslation
import com.example.techfeed.feature.translation.model.TranslationModelState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

@HiltViewModel
class NewsViewModel
@Inject
constructor(
    private val repository: NewsRepository,
    private val settingsRepository: SettingsRepository,
    private val translator: TranslationService,
) : ViewModel() {
    private val _state = MutableStateFlow(NewsState())
    val state = _state.asStateFlow()
    val saved =
        repository.saved
            .catch {
                _state.update { it.copy(error = UiText(R.string.error_saved_load)) }
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _settings = MutableStateFlow<NewsSettings?>(null)
    val settings = _settings.asStateFlow()
    private var fetchJob: kotlinx.coroutines.Job? = null
    private val _translations = MutableStateFlow<Map<String, ArticleTranslation>>(emptyMap())
    val translations = _translations.asStateFlow()
    private val _modelState = MutableStateFlow(TranslationModelState())
    val modelState = _modelState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                _modelState.value = TranslationModelState(ready = translator.isModelReady())
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _modelState.value =
                    TranslationModelState(
                        ready = false,
                        error = UiText(R.string.error_model_status),
                    )
            }
        }
        viewModelScope.launch {
            settingsRepository.settings
                .catch {
                    _state.update { it.copy(error = UiText(R.string.error_settings_load)) }
                }
                .collect { next ->
                    val sourcesChanged = _settings.value?.sources != next.sources
                    _settings.value = next
                    if (sourcesChanged) refresh()
                }
        }
    }

    fun setSource(name: String, enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setSource(name, enabled)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(error = UiText(R.string.error_source_save)) }
            }
        }
    }

    fun setTag(tag: String, visible: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setTag(tag, visible)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(error = UiText(R.string.error_tag_save)) }
            }
        }
    }

    fun moveTag(tag: String, offset: Int) = saveOrder { settingsRepository.moveTag(tag, offset) }

    fun moveSource(source: String, offset: Int) = saveOrder {
        settingsRepository.moveSource(source, offset)
    }

    private fun saveOrder(change: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                change()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(error = UiText(R.string.error_order_save)) }
            }
        }
    }

    fun downloadModel(thenTranslate: Article? = null) {
        if (_modelState.value.downloading) return
        if (_modelState.value.ready == true) {
            thenTranslate?.let { translate(it) }
            return
        }
        _modelState.value =
            TranslationModelState(ready = _modelState.value.ready, downloading = true)
        viewModelScope.launch {
            try {
                translator.downloadModel()
                _modelState.value = TranslationModelState(ready = true)
                thenTranslate?.let { translate(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _modelState.value =
                    TranslationModelState(
                        ready = false,
                        error = UiText(R.string.error_model_download),
                    )
            }
        }
    }

    fun translate(article: Article) {
        if (
            _modelState.value.ready != true ||
                !isEnglishArticle(article) ||
                _translations.value[article.id]?.loading == true ||
                _translations.value[article.id]?.title != null
        )
            return
        _translations.update { it + (article.id to ArticleTranslation(loading = true)) }
        viewModelScope.launch {
            try {
                val result = translator.translate(article)
                _translations.update { it + (article.id to result) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _translations.update {
                    it +
                        (article.id to
                            ArticleTranslation(
                                error = UiText(R.string.error_translate),
                            ))
                }
            }
        }
    }

    override fun onCleared() {
        translator.close()
        super.onCleared()
    }

    fun refresh() {
        val enabled = _settings.value?.sources ?: return
        fetchJob?.cancel()
        _state.update {
            it.copy(
                articles = it.articles.filter { article -> article.source in enabled },
                loading = true,
                error = null,
            )
        }
        fetchJob = viewModelScope.launch {
            val results = supervisorScope {
                feedSources
                    .filter { it.name in enabled }
                    .map { source ->
                        async {
                            try {
                                source to Result.success(repository.fetch(source))
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                source to Result.failure<List<Article>>(e)
                            }
                        }
                    }
                    .awaitAll()
            }
            _state.update { previous ->
                val result = resolveFeedResults(previous.articles, results)
                NewsState(
                    articles = result.articles,
                    error =
                        result.failedSources
                            .takeIf { it.isNotEmpty() }
                            ?.let {
                                UiText(R.string.error_feed_fetch, listOf(it.joinToString()))
                            },
                )
            }
        }
    }

    fun toggleSaved(article: Article) {
        viewModelScope.launch {
            try {
                repository.toggle(article)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(error = UiText(R.string.error_saved_change)) }
            }
        }
    }
}
