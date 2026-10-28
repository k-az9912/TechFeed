package com.example.techfeed.feature.news.data.remote

import com.example.techfeed.core.feed.FeedSource
import com.example.techfeed.core.model.Article
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import javax.inject.Inject

class FeedClient @Inject constructor(private val parser: FeedParser) {
    suspend fun fetch(source: FeedSource): List<Article> = withContext(Dispatchers.IO) {
        val connection = URL(source.url).openConnection() as HttpsURLConnection
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
                val bytes = output.toByteArray()
                check(bytes.size <= 2_000_000) { "Feed too large" }
                parser.parse(bytes.inputStream(), source).take(source.limit)
            }
        } finally {
            connection.disconnect()
        }
    }
}
