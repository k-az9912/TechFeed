package com.example.techfeed.feature.news.data.remote

import com.example.techfeed.core.feed.FeedSource
import com.example.techfeed.core.model.Article
import com.example.techfeed.core.network.safeFeedUrl
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.URL
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FeedClient @Inject constructor(private val parser: FeedParser) {
    suspend fun fetch(source: FeedSource): List<Article> =
        withContext(Dispatchers.IO) {
            val bytes = download(source.url)
            parser.parse(ByteArrayInputStream(bytes), source).take(source.limit)
        }

    suspend fun inspect(value: String): FeedSource =
        withContext(Dispatchers.IO) {
            val url = checkNotNull(safeFeedUrl(value)) { "Invalid HTTPS feed URL" }
            val placeholder = FeedSource("Custom feed", "ITニュース", url)
            val document = parser.parseDocument(ByteArrayInputStream(download(url)), placeholder)
            val fallback = checkNotNull(URI(url).host).removePrefix("www.")
            FeedSource(document.title.ifBlank { fallback }.take(80), "ITニュース", url)
        }

    private fun download(url: String): ByteArray {
        val connection = URL(url).openConnection() as HttpsURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("User-Agent", "TechFeed/1.0")
        try {
            check(connection.responseCode == 200) { "HTTP ${connection.responseCode}" }
            connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    check(output.size() + count <= 2_000_000) { "Feed too large" }
                    output.write(buffer, 0, count)
                }
                return output.toByteArray().also {
                    check(it.size <= 2_000_000) { "Feed too large" }
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}
