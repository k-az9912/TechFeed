package com.example.techfeed.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.techfeed.testsupport.publickeySecurity
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArticleDaoTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseName = "article-test-${UUID.randomUUID()}.db"
    private lateinit var database: NewsDatabase

    @Before
    fun openTestDatabase() {
        database = Room.databaseBuilder(context, NewsDatabase::class.java, databaseName).build()
    }

    @After
    fun removeOnlyTestDatabase() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun saveAndRemoveUpdateObservedArticles() = runBlocking {
        val dao = database.articles()
        assertTrue(dao.observe().first().isEmpty())

        dao.save(publickeySecurity)
        assertEquals(listOf(publickeySecurity), dao.observe().first())
        assertTrue(dao.contains(publickeySecurity.id))

        dao.remove(publickeySecurity.id)
        assertTrue(dao.observe().first().isEmpty())
        assertFalse(dao.contains(publickeySecurity.id))
    }

    @Test
    fun savedArticleSurvivesDatabaseReopen() = runBlocking {
        database.articles().save(publickeySecurity)
        database.close()
        database = Room.databaseBuilder(context, NewsDatabase::class.java, databaseName).build()

        assertEquals(listOf(publickeySecurity), database.articles().observe().first())
    }

    @Test
    fun togglingTwiceDoesNotLeaveDuplicates() = runBlocking {
        val dao = database.articles()
        dao.toggle(publickeySecurity)
        assertEquals(listOf(publickeySecurity), dao.observe().first())
        dao.toggle(publickeySecurity)
        assertTrue(dao.observe().first().isEmpty())
    }
}
