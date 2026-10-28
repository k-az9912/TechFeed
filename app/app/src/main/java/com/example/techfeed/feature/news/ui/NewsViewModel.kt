package com.example.techfeed.feature.news.ui

import com.example.techfeed.core.feed.feedSources
import com.example.techfeed.core.feed.isEnglishArticle
import com.example.techfeed.core.model.Article
import com.example.techfeed.feature.news.data.NewsRepository
import com.example.techfeed.feature.news.domain.orderArticles
import com.example.techfeed.feature.settings.data.SettingsRepository
import com.example.techfeed.feature.settings.model.NewsSettings
import com.example.techfeed.feature.translation.data.ArticleTranslator
import com.example.techfeed.feature.translation.model.ArticleTranslation
import com.example.techfeed.feature.translation.model.TranslationModelState
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val repository: NewsRepository,
    private val settingsRepository: SettingsRepository,
    private val translator: ArticleTranslator
) : ViewModel() {
    private val _state = MutableStateFlow(NewsState())
    val state = _state.asStateFlow()
    val saved = repository.saved.catch {
        _state.update { it.copy(error = "保存済み記事を読み込めませんでした。") }
        emit(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
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
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                _modelState.value = TranslationModelState(ready = false, error = "モデルの取得状態を確認できませんでした。")
            }
        }
        viewModelScope.launch {
            settingsRepository.settings.catch {
                _state.update { it.copy(error = "設定を読み込めませんでした。アプリを再起動してください。") }
            }.collect { next ->
                val sourcesChanged = _settings.value?.sources != next.sources
                _settings.value = next
                if (sourcesChanged) refresh()
            }
        }
    }

    fun setSource(name: String, enabled: Boolean) {
        viewModelScope.launch {
            try { settingsRepository.setSource(name, enabled) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { _state.update { it.copy(error = "取得元の設定を保存できませんでした。") } }
        }
    }

    fun setTag(tag: String, visible: Boolean) {
        viewModelScope.launch {
            try { settingsRepository.setTag(tag, visible) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { _state.update { it.copy(error = "タグの設定を保存できませんでした。") } }
        }
    }

    fun moveTag(tag: String, offset: Int) = saveOrder { settingsRepository.moveTag(tag, offset) }
    fun moveSource(source: String, offset: Int) = saveOrder { settingsRepository.moveSource(source, offset) }
    private fun saveOrder(change: suspend () -> Unit) {
        viewModelScope.launch {
            try { change() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { _state.update { it.copy(error = "表示順を保存できませんでした。") } }
        }
    }

    fun downloadModel(thenTranslate: Article? = null) {
        if (_modelState.value.downloading) return
        if (_modelState.value.ready == true) {
            thenTranslate?.let { translate(it) }
            return
        }
        _modelState.value = TranslationModelState(ready = _modelState.value.ready, downloading = true)
        viewModelScope.launch {
            try {
                translator.downloadModel()
                _modelState.value = TranslationModelState(ready = true)
                thenTranslate?.let { translate(it) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                _modelState.value = TranslationModelState(ready = false,
                    error = "モデルを取得できませんでした。通信と空き容量を確認して再試行してください。")
            }
        }
    }

    fun translate(article: Article) {
        if (_modelState.value.ready != true || !isEnglishArticle(article) || _translations.value[article.id]?.loading == true ||
            _translations.value[article.id]?.title != null) return
        _translations.update { it + (article.id to ArticleTranslation(loading = true)) }
        viewModelScope.launch {
            try {
                val result = translator.translate(article)
                _translations.update { it + (article.id to result) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                _translations.update { it + (article.id to ArticleTranslation(
                    error = "翻訳できませんでした。ネットワークと空き容量を確認し、再試行してください。")) }
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
        _state.update { it.copy(articles = it.articles.filter { article -> article.source in enabled }, loading = true, error = null) }
        fetchJob = viewModelScope.launch {
            val articles = mutableListOf<Article>()
            val failed = mutableListOf<String>()
            val results = supervisorScope {
                feedSources.filter { it.name in enabled }.map { source ->
                    async {
                        try { source to Result.success(repository.fetch(source)) }
                        catch (e: CancellationException) { throw e }
                        catch (e: Exception) { source to Result.failure<List<Article>>(e) }
                    }
                }.awaitAll()
            }
            results.forEach { (source, result) ->
                result.onSuccess { articles += it }.onFailure { failed += source.name }
            }
            _state.update {
                NewsState(
                    if (articles.isEmpty()) it.articles else orderArticles(articles),
                    error = failed.takeIf { it.isNotEmpty() }?.let { "取得できませんでした：${it.joinToString()}。再試行してください。" }
                )
            }
        }
    }

    fun toggleSaved(article: Article) {
        viewModelScope.launch {
            try {
                repository.toggle(article)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _state.update { it.copy(error = "保存状態を変更できませんでした。") } }
        }
    }
}
