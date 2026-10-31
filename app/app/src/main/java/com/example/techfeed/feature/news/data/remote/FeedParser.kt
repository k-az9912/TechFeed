package com.example.techfeed.feature.news.data.remote

import com.example.techfeed.core.feed.FeedSource
import com.example.techfeed.core.model.Article
import com.example.techfeed.core.network.safeArticleUrl
import java.io.InputStream
import javax.inject.Inject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/** RSS 2.0 / Atom の直下のフィールドだけを読む。外部エンティティは解決しない。 */
class FeedParser @Inject constructor() {
    data class Document(val title: String, val articles: List<Article>)

    fun parse(input: InputStream, source: FeedSource): List<Article> =
        parseDocument(input, source).articles

    fun parseDocument(input: InputStream, source: FeedSource): Document {
        val parser =
            XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }.newPullParser()
        if (parser.getFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL)) {
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false)
        }
        parser.setInput(input, null)
        val result = mutableListOf<Article>()
        var rootName = ""
        var channelDepth = -1
        var feedTitle = ""
        var depth = -1
        var title = ""
        var summary = ""
        var link = ""
        var date = ""
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.DOCDECL) error("DOCTYPE is unsupported")
            if (parser.eventType == XmlPullParser.START_TAG) {
                val tag = parser.name
                if (rootName.isEmpty()) {
                    rootName = tag.lowercase()
                    check(rootName in setOf("rss", "feed", "rdf")) { "Unsupported feed XML" }
                }
                if (depth == -1 && tag == "channel") channelDepth = parser.depth
                if (tag in listOf("item", "entry") && depth == -1) {
                    depth = parser.depth
                    title = ""
                    summary = ""
                    link = ""
                    date = ""
                } else if (
                    depth == -1 &&
                        tag == "title" &&
                        (parser.depth == channelDepth + 1 ||
                            (rootName == "feed" && parser.depth == 2))
                ) {
                    feedTitle = plain(readText(parser))
                } else if (depth >= 0 && parser.depth == depth + 1) {
                    when (tag) {
                        "title" -> title = readText(parser)
                        "summary",
                        "description" -> summary = readText(parser)
                        "published",
                        "pubDate",
                        "date" -> date = readText(parser)
                        "updated" -> {
                            val value = readText(parser)
                            if (date.isEmpty()) date = value
                        }
                        "link" -> {
                            val href = parser.getAttributeValue(null, "href")
                            val rel = parser.getAttributeValue(null, "rel")
                            if (href != null) {
                                if (rel == null || rel == "alternate") link = href
                            } else link = readText(parser)
                        }
                    }
                }
            } else if (parser.eventType == XmlPullParser.END_TAG && parser.depth == depth) {
                val url = safeArticleUrl(link)
                if (title.isNotBlank() && url != null) {
                    result +=
                        Article(
                            url,
                            source.name,
                            source.category,
                            plain(title),
                            plain(summary).take(500),
                            date,
                            url,
                        )
                }
                depth = -1
            } else if (
                parser.eventType == XmlPullParser.END_TAG &&
                    parser.name == "channel" &&
                    parser.depth == channelDepth
            ) {
                channelDepth = -1
            }
            parser.nextToken()
        }
        check(rootName.isNotEmpty()) { "Empty feed XML" }
        check(depth == -1 && parser.depth == 0) { "Incomplete feed XML" }
        return Document(feedTitle, result.distinctBy { it.id })
    }

    private fun readText(parser: XmlPullParser): String {
        val depth = parser.depth
        val value = StringBuilder()
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.END_TAG && parser.depth == depth) break
            if (parser.eventType == XmlPullParser.TEXT) value.append(parser.text)
        }
        return value.toString()
    }

    private fun plain(value: String) =
        value
            .replace(Regex("<[^>]*>"), " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&nbsp;", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}
