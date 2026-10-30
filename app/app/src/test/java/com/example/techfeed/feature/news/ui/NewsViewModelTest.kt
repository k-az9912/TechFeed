package com.example.techfeed.feature.news.ui

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.lifecycle.ViewModelStore
import com.example.techfeed.R
import com.example.techfeed.core.feed.FeedSource
import com.example.techfeed.core.feed.availableSources
import com.example.techfeed.core.model.Article
import com.example.techfeed.feature.news.data.NewsRepository
import com.example.techfeed.feature.settings.data.SettingsRepository
import com.example.techfeed.feature.translation.domain.TranslationService
import com.example.techfeed.feature.translation.model.ArticleTranslation
import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NewsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val viewModels = ViewModelStore()
    private val repository = FakeNewsRepository()
    private val translator = FakeTranslationService()
    private val settings = MemorySettings()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        viewModels.clear()
        Dispatchers.resetMain()
    }

    private fun createViewModel(): NewsViewModel =
        NewsViewModel(repository, SettingsRepository(settings), translator).also {
            viewModels.put("news", it)
        }

    @Test
    fun translationFailureResetsLoadingAndRetryReplacesErrorWithResult() =
        runTest(dispatcher) {
            val vm = createViewModel()
            runCurrent()
            translator.translateAction = { throw IOException("inference failed") }
            vm.translate(english)
            assertTrue(vm.translations.value.getValue(english.id).loading)
            runCurrent()
            val failed = vm.translations.value.getValue(english.id)
            assertFalse(failed.loading)
            assertNull(failed.title)
            assertEquals(R.string.error_translate, failed.error?.resourceId)

            translator.translateAction = { translated }
            vm.translate(english)
            assertNull(vm.translations.value.getValue(english.id).error)
            runCurrent()
            assertEquals(translated, vm.translations.value[english.id])
            assertEquals(2, translator.translationCalls)
        }

    @Test
    fun duplicateTranslationIsIgnoredWhileLoadingAndAfterSuccess() =
        runTest(dispatcher) {
            val response = CompletableDeferred<ArticleTranslation>()
            translator.translateAction = { response.await() }
            val vm = createViewModel()
            runCurrent()
            vm.translate(english)
            vm.translate(english)
            runCurrent()
            assertEquals(1, translator.translationCalls)
            assertTrue(vm.translations.value.getValue(english.id).loading)
            vm.translate(english)
            response.complete(translated)
            runCurrent()
            vm.translate(english)
            runCurrent()
            assertEquals(1, translator.translationCalls)
            assertEquals(translated, vm.translations.value[english.id])
        }

    @Test
    fun translationRequiresReadyModelAndEnglishSource() =
        runTest(dispatcher) {
            translator.ready = false
            val vm = createViewModel()
            vm.translate(english)
            runCurrent()
            vm.translate(english)
            vm.downloadModel()
            runCurrent()
            vm.translate(english.copy(source = "Publickey"))
            runCurrent()
            assertEquals(0, translator.translationCalls)
            assertTrue(vm.translations.value.isEmpty())
        }

    @Test
    fun downloadFailureAllowsRetryAndOnlyTranslatesAfterSuccess() =
        runTest(dispatcher) {
            translator.ready = false
            translator.downloadAction = { throw IOException("download failed") }
            val vm = createViewModel()
            runCurrent()
            vm.downloadModel(english)
            assertTrue(vm.modelState.value.downloading)
            runCurrent()
            assertEquals(false, vm.modelState.value.ready)
            assertFalse(vm.modelState.value.downloading)
            assertEquals(R.string.error_model_download, vm.modelState.value.error?.resourceId)
            assertEquals(0, translator.translationCalls)

            translator.downloadAction = {}
            vm.downloadModel(english)
            assertNull(vm.modelState.value.error)
            runCurrent()
            assertEquals(true, vm.modelState.value.ready)
            assertFalse(vm.modelState.value.downloading)
            assertEquals(translated, vm.translations.value[english.id])
            assertEquals(2, translator.downloadCalls)
            assertEquals(1, translator.translationCalls)
        }

    @Test
    fun duplicateDownloadDoesNotStartAnotherRequest() =
        runTest(dispatcher) {
            translator.ready = false
            val finished = CompletableDeferred<Unit>()
            translator.downloadAction = { finished.await() }
            val vm = createViewModel()
            runCurrent()
            vm.downloadModel()
            vm.downloadModel()
            runCurrent()
            assertEquals(1, translator.downloadCalls)
            assertTrue(vm.modelState.value.downloading)
            vm.downloadModel()
            finished.complete(Unit)
            runCurrent()
            vm.downloadModel()
            runCurrent()
            assertEquals(1, translator.downloadCalls)
            assertEquals(true, vm.modelState.value.ready)
        }

    @Test
    fun modelStatusFailureCanRecoverByDownloading() =
        runTest(dispatcher) {
            translator.statusAction = { throw IOException("status unavailable") }
            val vm = createViewModel()
            runCurrent()
            assertEquals(R.string.error_model_status, vm.modelState.value.error?.resourceId)
            assertEquals(false, vm.modelState.value.ready)
            vm.downloadModel()
            runCurrent()
            assertEquals(true, vm.modelState.value.ready)
            assertNull(vm.modelState.value.error)
        }

    @Test
    fun sourceChangeCancelsOldRefreshAndLateResponseCannotOverwriteNewArticles() =
        runTest(dispatcher) {
            settings.enableOnly("Publickey")
            val oldResponse = CompletableDeferred<List<Article>>()
            var oldCancelled = false
            repository.fetchAction = { source ->
                if (source.name == "Publickey") {
                    try {
                        // Simulate an I/O call that still finishes after its caller is cancelled.
                        withContext(NonCancellable) { oldResponse.await() }
                    } finally {
                        oldCancelled = !currentCoroutineContext().isActive
                    }
                } else listOf(fresh)
            }
            val vm = createViewModel()
            runCurrent()
            assertEquals(listOf("Publickey"), repository.requests)
            vm.setSource("Publickey", false)
            vm.setSource("Ars Technica", true)
            runCurrent()
            assertEquals(listOf(fresh), vm.state.value.articles)
            assertFalse(vm.state.value.loading)
            oldResponse.complete(listOf(english.copy(source = "Publickey", title = "古い応答")))
            runCurrent()
            assertTrue(oldCancelled)
            assertEquals(listOf(fresh), vm.state.value.articles)
            assertNull(vm.state.value.error)
        }

    @Test
    fun repeatedRefreshCancelsPreviousRequest() =
        runTest(dispatcher) {
            settings.enableOnly("Ars Technica")
            var cancelled = false
            var calls = 0
            repository.fetchAction = {
                if (++calls == 1) {
                    try {
                        awaitCancellation()
                    } finally {
                        cancelled = true
                    }
                } else listOf(fresh)
            }
            val vm = createViewModel()
            runCurrent()
            vm.refresh()
            runCurrent()
            assertTrue(cancelled)
            assertEquals(2, calls)
            assertEquals(listOf(fresh), vm.state.value.articles)
            assertFalse(vm.state.value.loading)
        }

    @Test
    fun allSourcesDisabledClearsArticlesWithoutFetchingAgain() =
        runTest(dispatcher) {
            settings.enableOnly("Ars Technica")
            repository.fetchAction = { listOf(fresh) }
            val vm = createViewModel()
            runCurrent()
            assertEquals(listOf(fresh), vm.state.value.articles)
            vm.setSource("Ars Technica", false)
            runCurrent()
            assertTrue(vm.state.value.articles.isEmpty())
            assertFalse(vm.state.value.loading)
            assertNull(vm.state.value.error)
            assertEquals(1, repository.requests.size)
        }

    @Test
    fun refreshFailureKeepsPreviousArticlesAndSuccessfulRetryClearsError() =
        runTest(dispatcher) {
            settings.enableOnly("Ars Technica")
            repository.fetchAction = { listOf(english) }
            val vm = createViewModel()
            runCurrent()
            repository.fetchAction = { throw IOException("offline") }
            vm.refresh()
            runCurrent()
            assertEquals(listOf(english), vm.state.value.articles)
            assertFalse(vm.state.value.loading)
            assertEquals(R.string.error_feed_fetch, vm.state.value.error?.resourceId)
            repository.fetchAction = { listOf(fresh) }
            vm.refresh()
            runCurrent()
            assertEquals(listOf(fresh), vm.state.value.articles)
            assertNull(vm.state.value.error)
        }

    @Test
    fun clearingViewModelClosesTranslatorAndCancelsTranslation() =
        runTest(dispatcher) {
            var cancelled = false
            translator.translateAction = {
                try {
                    awaitCancellation()
                } finally {
                    cancelled = true
                }
            }
            val vm = createViewModel()
            runCurrent()
            vm.translate(english)
            runCurrent()
            viewModels.clear()
            runCurrent()
            assertTrue(cancelled)
            assertTrue(translator.closed)
        }

    private class MemorySettings : DataStore<Preferences> {
        override val data = MutableStateFlow<Preferences>(preferencesOf())

        override suspend fun updateData(
            transform: suspend (Preferences) -> Preferences,
        ): Preferences = transform(data.value).also { data.value = it }

        fun enableOnly(vararg sources: String) {
            data.value =
                preferencesOf(
                    stringSetPreferencesKey("disabled_sources") to
                        (availableSources.toSet() - sources.toSet()),
                )
        }
    }

    private class FakeNewsRepository : NewsRepository {
        override val saved = MutableStateFlow<List<Article>>(emptyList())
        val requests = mutableListOf<String>()
        var fetchAction: suspend (FeedSource) -> List<Article> = { emptyList() }

        override suspend fun fetch(source: FeedSource): List<Article> {
            requests += source.name
            return fetchAction(source)
        }

        override suspend fun toggle(article: Article) {}
    }

    private class FakeTranslationService : TranslationService {
        var ready = true
        var statusAction: suspend () -> Boolean = { ready }
        var downloadAction: suspend () -> Unit = {}
        var translateAction: suspend (Article) -> ArticleTranslation = { translated }
        var downloadCalls = 0
        var translationCalls = 0
        var closed = false

        override suspend fun isModelReady() = statusAction()

        override suspend fun downloadModel() {
            downloadCalls++
            downloadAction()
        }

        override suspend fun translate(article: Article): ArticleTranslation {
            translationCalls++
            return translateAction(article)
        }

        override fun close() {
            closed = true
        }
    }

    companion object {
        private val english =
            Article(
                "https://example.com/old",
                "Ars Technica",
                "海外IT",
                "Original",
                "Summary",
                "2026-10-30",
            )
        private val fresh = english.copy(id = "https://example.com/new", title = "New headline")
        private val translated = ArticleTranslation(title = "翻訳結果", summary = "翻訳した概要")
    }
}
