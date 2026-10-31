package com.example.techfeed.feature.settings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.techfeed.core.feed.availableSources
import com.example.techfeed.core.feed.FeedSource
import com.example.techfeed.feature.settings.model.NewsSettings
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val testFile =
        File(context.cacheDir, "settings-test-${UUID.randomUUID()}.preferences_pb")
    private lateinit var storeJob: CompletableJob
    private lateinit var store: DataStore<Preferences>
    private lateinit var repository: SettingsRepository

    @Before fun openTestStore() = createStore()

    private fun createStore() {
        storeJob = SupervisorJob()
        store =
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(Dispatchers.IO + storeJob),
                produceFile = { testFile },
            )
        repository = SettingsRepository(store)
    }

    private suspend fun reopenStore() {
        // A file must never be opened by two active DataStore instances.
        storeJob.cancelAndJoin()
        createStore()
    }

    @After
    fun removeOnlyTestStore() = runBlocking {
        storeJob.cancelAndJoin()
        testFile.delete()
        Unit
    }

    @Test
    fun sourceAndTagChoicesSurviveStoreRecreation() = runBlocking {
        assertEquals(NewsSettings(), repository.settings.first())
        repository.setSource("Qiita", false)
        repository.setTag("AI", false)
        val expected = repository.settings.first()
        reopenStore()

        assertEquals(expected, repository.settings.first())
        assertFalse("Qiita" in repository.settings.first().sources)
        assertFalse("AI" in repository.settings.first().visibleTags)
    }

    @Test
    fun displayOrdersSurviveStoreRecreation() = runBlocking {
        repository.moveTag("AI", -1)
        repository.moveSource("Qiita", -1)
        reopenStore()

        val restored = repository.settings.first()
        assertEquals("AI", restored.tagOrder.first())
        assertEquals("Qiita", restored.sourceOrder.first())
    }

    @Test
    fun explicitlyDisablingAllSourcesDoesNotRestoreDefaults() = runBlocking {
        availableSources.forEach { repository.setSource(it, false) }
        reopenStore()
        assertTrue(repository.settings.first().sources.isEmpty())
    }


    @Test
    fun customSourceSurvivesRecreationAndCanBeRemoved() = runBlocking {
        repository.addCustomSource(
            FeedSource("Example News", "ITニュース", "https://example.com/feed.xml"),
        )
        reopenStore()
        val restored = repository.settings.first()
        assertEquals("Example News", restored.feeds.last().name)
        assertTrue("Example News" in restored.sources)

        repository.removeCustomSource("Example News")
        reopenStore()
        assertFalse(repository.settings.first().feeds.any { it.name == "Example News" })
    }

    @Test
    fun customTagSurvivesRecreationAndCanBeRemoved() = runBlocking {
        repository.addCustomTag("Compose", listOf("Jetpack Compose", "Compose Multiplatform"))
        reopenStore()
        val restored = repository.settings.first()
        val tag = restored.tags.single { it.name == "Compose" }
        assertEquals(listOf("Jetpack Compose", "Compose Multiplatform"), tag.keywords)
        assertTrue(tag.custom)
        assertTrue("Compose" in restored.visibleTags)

        repository.removeCustomTag("Compose")
        reopenStore()
        assertFalse(repository.settings.first().tags.any { it.name == "Compose" })
    }
}
